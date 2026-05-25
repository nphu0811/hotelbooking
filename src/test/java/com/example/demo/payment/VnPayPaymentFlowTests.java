package com.example.demo.payment;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.RefundStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PaymentWebhookEventRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.payment.provider=vnpay",
        "vnpay.tmn-code=TESTTMNCODE",
        "vnpay.hash-secret=test-vnpay-hmac-secret",
        "vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
        "vnpay.return-url=https://staging.example.test/payments/vnpay/return",
        "vnpay.ipn-url=https://staging.example.test/payments/vnpay/webhook"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VnPayPaymentFlowTests {
    private static final String HASH_SECRET = "test-vnpay-hmac-secret";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUpUser() {
        Role role = roleRepository.findByCode("USER")
                .orElseGet(() -> roleRepository.save(new Role("USER", "Default customer role")));
        user = userRepository.findByEmailIgnoreCase("vnpay-customer@example.test").orElseGet(User::new);
        user.setFullName("VNPay Customer");
        user.setEmail("vnpay-customer@example.test");
        user.setPhone("0900000000");
        user.setPasswordHash(passwordEncoder.encode("User@123"));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.getRoles().clear();
        user.getRoles().add(role);
        user = userRepository.save(user);
    }

    @Test
    void createsSignedPaymentUrlAndAcceptsIdempotentValidWebhook() {
        Booking booking = createBooking(101);
        PaymentIntent intent = paymentService.createPaymentIntent(booking);
        assertThat(intent.provider()).isEqualTo("vnpay");
        assertThat(intent.redirectUrl()).contains("vnp_TmnCode=TESTTMNCODE", "vnp_SecureHash=");

        String rawPayload = signedPayload(intent.orderId(), booking.getTotalAmount(), "VND", "TXN-OK-1");
        var payment = paymentService.handleWebhook("vnpay", Map.of(), rawPayload);
        var duplicate = paymentService.handleWebhook("vnpay", Map.of(), rawPayload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getProviderTransactionId()).isEqualTo("TXN-OK-1");
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(duplicate.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(webhookEventRepository.findAll().stream()
                .filter(event -> event.getProviderEventId().contains("TXN-OK-1"))
                .count()).isEqualTo(1);
    }

    @Test
    void anonymousWebhookHttpEndpointProcessesSignedVnPayIpnWithoutCsrf() throws Exception {
        Booking booking = createBooking(106);
        PaymentIntent intent = paymentService.createPaymentIntent(booking);
        String rawPayload = signedPayload(intent.orderId(), booking.getTotalAmount(), "VND", "TXN-HTTP-IPN");

        mockMvc.perform(post("/payments/vnpay/webhook")
                        .contentType("application/x-www-form-urlencoded")
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("PAID"));

        assertThat(paymentRepository.findByOrderId(intent.orderId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void rejectsInvalidSignatureAndKeepsPaymentPending() {
        Booking booking = createBooking(111);
        PaymentIntent intent = paymentService.createPaymentIntent(booking);
        String validPayload = signedPayload(intent.orderId(), booking.getTotalAmount(), "VND", "TXN-BAD-SIG");
        String invalidPayload = validPayload.substring(0, validPayload.indexOf("&vnp_SecureHash="))
                + "&vnp_SecureHash=bad";

        assertThatThrownBy(() -> paymentService.handleWebhook("vnpay", Map.of(), invalidPayload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("signature");

        var savedPayment = paymentRepository.findByOrderId(intent.orderId()).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(webhookEventRepository.findAll().stream()
                .anyMatch(event -> !event.isSignatureValid() && !event.isProcessed()))
                .isTrue();
    }

    @Test
    void rejectsWrongAmountAndWrongCurrency() {
        Booking amountBooking = createBooking(121);
        PaymentIntent amountIntent = paymentService.createPaymentIntent(amountBooking);
        String wrongAmount = signedPayload(amountIntent.orderId(),
                amountBooking.getTotalAmount().add(BigDecimal.valueOf(1_000)), "VND", "TXN-WRONG-AMOUNT");

        assertThatThrownBy(() -> paymentService.handleWebhook("vnpay", Map.of(), wrongAmount))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("amount");
        assertThat(paymentRepository.findByOrderId(amountIntent.orderId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);

        Booking currencyBooking = createBooking(131);
        PaymentIntent currencyIntent = paymentService.createPaymentIntent(currencyBooking);
        String wrongCurrency = signedPayload(currencyIntent.orderId(),
                currencyBooking.getTotalAmount(), "USD", "TXN-WRONG-CURRENCY");

        assertThatThrownBy(() -> paymentService.handleWebhook("vnpay", Map.of(), wrongCurrency))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("currency");
        assertThat(paymentRepository.findByOrderId(currencyIntent.orderId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void refundRequestMovesToProcessingForVnPaySettlement() {
        Booking booking = createBooking(141);
        PaymentIntent intent = paymentService.createPaymentIntent(booking);
        paymentService.handleWebhook("vnpay", Map.of(),
                signedPayload(intent.orderId(), booking.getTotalAmount(), "VND", "TXN-REFUND"));

        var refund = bookingService.cancel(user, booking.getId());

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        assertThat(refund.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(refund.getBooking().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    private Booking createBooking(int dayOffset) {
        var room = roomRepository.findAll().stream()
                .filter(candidate -> candidate.getStatus() == RoomStatus.AVAILABLE)
                .findFirst()
                .orElseThrow();
        return bookingService.createPendingBooking(
                user,
                room.getId(),
                LocalDate.now().plusDays(dayOffset),
                LocalDate.now().plusDays(dayOffset + 2),
                2,
                null
        );
    }

    private String signedPayload(String orderId, BigDecimal amount, String currency, String transactionNo) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", currency);
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TransactionNo", transactionNo);
        String canonical = canonical(params);
        return canonical + "&vnp_SecureHash=" + encode(hmacSha512(HASH_SECRET, canonical));
    }

    private String canonical(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign fixture", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
