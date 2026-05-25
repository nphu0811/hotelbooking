package com.example.demo;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.EmailStatus;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.RefundStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.hoteldata.HotelDataRecord;
import com.example.demo.hoteldata.HotelImportService;
import com.example.demo.hoteldata.HotelUpsertService;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.EmailJobRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.EmailService;
import com.example.demo.service.PaymentService;
import com.example.demo.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
class HotelBookingApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailJobRepository emailJobRepository;

    @Autowired
    private HotelUpsertService hotelUpsertService;

    @Autowired
    private HotelImportService hotelImportService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void ensureTestAccounts() {
        Role userRole = roleRepository.findByCode("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER", "Default customer role")));
        Role adminRole = roleRepository.findByCode("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN", "Hotel administrator")));
        Role superAdminRole = roleRepository.findByCode("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("SUPER_ADMIN", "Super administrator")));

        upsertTestUser("customer@example.test", "Test Customer", "0912345678", "User@123", userRole);
        upsertTestUser("admin@example.test", "Test Admin", "0987654321", "Admin@123", adminRole, superAdminRole);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void jacksonSerializesInstantAsUtcText() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("timestamp", Instant.parse("2026-05-23T18:00:00Z")));

        assertThat(json).contains("2026-05-23T18:00:00Z");
    }

    @Test
    void homeAndSearchRenderSeedRooms() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));

        mockMvc.perform(get("/rooms/search")
                        .param("q", "Đà Nẵng")
                        .param("checkIn", LocalDate.now().plusDays(1).toString())
                        .param("checkOut", LocalDate.now().plusDays(3).toString())
                        .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/search"));
    }

    @Test
    void unauthenticatedBookingRedirectsToLogin() throws Exception {
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post("/bookings")
                        .with(csrf())
                        .param("roomId", room.getId().toString())
                        .param("checkIn", LocalDate.now().plusDays(5).toString())
                        .param("checkOut", LocalDate.now().plusDays(7).toString())
                        .param("guests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "customer@example.test", roles = "USER")
    void customerCanCreatePendingBooking() throws Exception {
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post("/bookings")
                        .with(csrf())
                        .param("roomId", room.getId().toString())
                        .param("checkIn", LocalDate.now().plusDays(11).toString())
                        .param("checkOut", LocalDate.now().plusDays(13).toString())
                        .param("guests", "2"))
                .andExpect(status().is3xxRedirection());

        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getRoom().getId().equals(room.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void overlappingBookingIsRejected() {
        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        LocalDate checkIn = LocalDate.now().plusDays(21);
        LocalDate checkOut = checkIn.plusDays(2);

        Booking first = bookingService.createPendingBooking(user, room.getId(), checkIn, checkOut, 2, null);

        assertThat(first.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> bookingService.createPendingBooking(
                user, room.getId(), checkIn.plusDays(1), checkOut.plusDays(1), 2, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phòng");
    }

    @Test
    void mockPaymentCallbackIsIdempotent() {
        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        Booking booking = bookingService.createPendingBooking(
                user, room.getId(), LocalDate.now().plusDays(31), LocalDate.now().plusDays(33), 2, null);
        var payment = paymentService.startMockPayment(booking);

        var confirmed = paymentService.completeLocalMockPayment(payment.getOrderId(), true);
        var duplicate = paymentService.mockCallback(payment.getOrderId(), false, "bad");

        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmed.getBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(duplicate.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(duplicate.getSignatureValid()).isTrue();
    }

    @Test
    void consoleEmailProviderMarksSentAfterProcessing() {
        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        var job = emailService.enqueue(user, null, EmailEventType.EMAIL_VERIFICATION,
                user.getEmail(), "Verify account", "email-verification");

        for (int attempt = 0; attempt < 5; attempt++) {
            emailService.processPendingEmails();
            if (emailJobRepository.findById(job.getId()).orElseThrow().getStatus() == EmailStatus.SENT) {
                break;
            }
        }

        var saved = emailJobRepository.findById(job.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(saved.getProviderMessageId()).startsWith("CONSOLE-");
    }

    @Test
    void registrationStoresHashedVerificationTokenAndAppliesResendCooldown() {
        String email = "Phase4-" + UUID.randomUUID() + "@Example.TEST";

        User user = authService.register("Phase Four User", email, "0911111111", "User@123", "User@123");
        String firstHash = user.getEmailVerificationTokenHash();

        User duplicate = authService.register("Phase Four User", email, "0911111111", "User@123", "User@123");

        assertThat(userRepository.findByEmailIgnoreCase(email.toLowerCase()).orElseThrow().getEmail())
                .isEqualTo(email.toLowerCase());
        assertThat(firstHash).hasSize(64);
        assertThat(firstHash).doesNotContain("-");
        var verificationJob = emailJobRepository.findAll().stream()
                .filter(job -> job.getRecipient().equals(email.toLowerCase()))
                .findFirst()
                .orElseThrow();
        assertThat(verificationJob.getBodyText()).contains("http://localhost:8080/verify/");
        assertThat(verificationJob.getBodyText()).doesNotContain(firstHash);
        assertThat(duplicate.getEmailVerificationTokenHash()).isEqualTo(firstHash);
        assertThat(duplicate.getEmailVerificationLastSentAt()).isNotNull();
    }

    @Test
    void paidMockBookingCancellationCreatesCompletedRefund() {
        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        Booking booking = bookingService.createPendingBooking(
                user, room.getId(), LocalDate.now().plusDays(41), LocalDate.now().plusDays(43), 2, null);
        var payment = paymentService.startMockPayment(booking);
        paymentService.completeLocalMockPayment(payment.getOrderId(), true);

        var refund = bookingService.cancel(user, booking.getId());

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(refund.getBooking().getStatus()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(refund.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void hotelUpsertStoresSourceProvenanceAndInternalRoomTemplate() {
        HotelDataRecord record = new HotelDataRecord(
                "overpass",
                "node/123456789",
                "Fixture Legal Hotel",
                "1 Test Street",
                "Ho Chi Minh City",
                "Ho Chi Minh City",
                "VN",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                4,
                "+84123456789",
                "https://fixture.example.test",
                "https://www.openstreetmap.org/node/123456789",
                "/css/room-placeholder.svg",
                "Fixture image",
                Map.of("internet_access", "Internet access"),
                "{\"id\":123456789}"
        );

        var first = hotelUpsertService.upsert(record, false);
        var second = hotelUpsertService.upsert(record, false);

        var hotel = hotelRepository.findBySourceAndSourceExternalId("OVERPASS", "node/123456789").orElseThrow();
        assertThat(first.inserted()).isTrue();
        assertThat(second.updated()).isTrue();
        assertThat(hotel.getSource()).isEqualTo("OVERPASS");
        assertThat(hotel.getSourceUrl()).contains("openstreetmap.org");
        var room = roomRepository.findAll().stream()
                .filter(candidate -> candidate.getHotel().getId().equals(hotel.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(room.getRoomSource()).isEqualTo("INTERNAL_TEMPLATE");
        assertThat(room.getRateSource()).isEqualTo("INTERNAL_ESTIMATE");
    }

    @Test
    void geoapifyHotelUpsertStoresProvenanceAndDeduplicatesAgainstOsm() {
        // Use Hanoi coordinates isolated from other tests that reuse HCMC fixture coords.
        BigDecimal osmLat = new BigDecimal("21.0285000");
        BigDecimal osmLng = new BigDecimal("105.8542000");

        // 1. Create an existing OSM (OVERPASS) hotel
        HotelDataRecord osmRecord = new HotelDataRecord(
                "overpass",
                "node/osm-dedup-test",
                "Deduplication Test Hotel",
                "123 OSM St",
                "Ha Noi",
                "Ha Noi",
                "VN",
                osmLat,
                osmLng,
                3,
                "+84111111111",
                "https://osm.example.test",
                "https://www.openstreetmap.org/node/osm-dedup-test",
                null,
                null,
                Map.of(),
                "{\"id\":\"osm-dedup-test\"}"
        );
        var osmUpsertResult = hotelUpsertService.upsert(osmRecord, false);
        assertThat(osmUpsertResult.inserted()).isTrue();

        // Verify it was saved as OVERPASS with coordinates for proximity dedup
        var savedOsm = hotelRepository.findBySourceAndSourceExternalId("OVERPASS", "node/osm-dedup-test").orElseThrow();
        assertThat(savedOsm.getSource()).isEqualTo("OVERPASS");
        assertThat(savedOsm.getLatitude()).isEqualByComparingTo(osmLat);
        assertThat(savedOsm.getLongitude()).isEqualByComparingTo(osmLng);
        hotelRepository.flush();

        // 2. Create a Geoapify record with the same name and within coordinate proximity window (0.0007)
        HotelDataRecord geoapifyRecord = new HotelDataRecord(
                "geoapify",
                "place_id_geoapify_dedup_test",
                "Deduplication Test Hotel", // Same name
                "123 Geoapify St",
                "Ha Noi",
                "Ha Noi",
                "VN",
                osmLat.add(new BigDecimal("0.0002000")),
                osmLng.add(new BigDecimal("0.0002000")),
                null,
                "+84222222222",
                "https://geoapify.example.test",
                "https://geoapify.example.test/place/123",
                null,
                null,
                Map.of("internet_access", "Internet access"),
                "{\"place_id\":\"place_id_geoapify_dedup_test\"}"
        );

        // 3. Upsert the Geoapify record and verify it updates the OSM record (deduplicated)
        var geoapifyUpsertResult = hotelUpsertService.upsert(geoapifyRecord, false);
        assertThat(geoapifyUpsertResult.updated()).isTrue();

        // 4. Verify that the original hotel now has GEOAPIFY source and ID, and is updated
        var updatedHotel = hotelRepository.findById(savedOsm.getId()).orElseThrow();
        assertThat(updatedHotel.getSource()).isEqualTo("GEOAPIFY");
        assertThat(updatedHotel.getSourceExternalId()).isEqualTo("place_id_geoapify_dedup_test");
        assertThat(updatedHotel.getSourceUrl()).isEqualTo("https://geoapify.example.test/place/123");
        assertThat(updatedHotel.getPhone()).isEqualTo("+84222222222");
    }

    @Test
    void hotelImportServiceSanitizesSensitiveKeys() {
        String rawError = "Request to https://api.geoapify.com/v2/places?apiKey=not-a-real-test-key failed: invalid key not-a-real-test-key";
        String sanitized = hotelImportService.sanitize(rawError);
        assertThat(sanitized).doesNotContain("not-a-real-test-key");
        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    @WithMockUser(username = "customer@example.test", roles = "USER")
    void userCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void userCannotOpenAnotherUsersBookingDetail() throws Exception {
        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        var room = roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        Booking booking = bookingService.createPendingBooking(
                user, room.getId(), LocalDate.now().plusDays(51), LocalDate.now().plusDays(53), 2, null);

        mockMvc.perform(get("/account/bookings/{id}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    @Test
    @WithMockUser(username = "customer@example.test", roles = "USER")
    void userCannotPostAdminBookingActions() throws Exception {
        mockMvc.perform(post("/admin/bookings/{id}/check-in", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.test", roles = "ADMIN")
    void adminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "customer@example.test", roles = "USER")
    void logoutAcceptsCsrfAndRedirectsHome() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void repeatedLoginFailuresShowCaptchaAndLockAccount() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "customer@example.test")
                            .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern(attempt >= 3 ? "/login?error&captcha" : "/login?error"));
        }

        mockMvc.perform(get("/login").param("error", "").param("captcha", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("captchaRequired", true));

        for (int attempt = 4; attempt <= 5; attempt++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "customer@example.test")
                            .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection());
        }

        var user = userRepository.findByEmailIgnoreCase("customer@example.test").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();

        mockMvc.perform(get("/login").param("error", "").param("locked", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("locked", true));
    }

    private void upsertTestUser(String email, String fullName, String phone, String password, Role... roles) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setLockReason(null);
        user.getRoles().clear();
        for (Role role : roles) {
            user.getRoles().add(role);
        }
        userRepository.save(user);
    }
}
