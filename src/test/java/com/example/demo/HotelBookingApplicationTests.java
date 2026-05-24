package com.example.demo;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

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
class HotelBookingApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

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

        var confirmed = paymentService.mockCallback(payment.getOrderId(), true, "mock-secret");
        var duplicate = paymentService.mockCallback(payment.getOrderId(), false, "bad");

        assertThat(confirmed.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(confirmed.getBooking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(duplicate.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(duplicate.getSignatureValid()).isTrue();
    }

    @Test
    @WithMockUser(username = "customer@example.test", roles = "USER")
    void userCannotOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
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
}
