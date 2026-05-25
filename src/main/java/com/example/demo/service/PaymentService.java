package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.entity.PaymentWebhookEvent;
import com.example.demo.entity.RefundRequest;
import com.example.demo.entity.RefundStatus;
import com.example.demo.payment.PaymentIntent;
import com.example.demo.payment.PaymentIntentRequest;
import com.example.demo.payment.PaymentProvider;
import com.example.demo.payment.PaymentProviderRegistry;
import com.example.demo.payment.PaymentWebhookResult;
import com.example.demo.payment.PaymentWebhookPayload;
import com.example.demo.payment.RefundCommand;
import com.example.demo.payment.RefundResult;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PaymentWebhookEventRepository;
import com.example.demo.repository.RefundRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final EmailService emailService;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final boolean mockPaymentEnabled;
    private final String mockPaymentSignature;
    private final String configuredPaymentProvider;
    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          PaymentWebhookEventRepository webhookEventRepository,
                          RefundRequestRepository refundRequestRepository,
                          EmailService emailService,
                          PaymentProviderRegistry paymentProviderRegistry,
                          @Value("${app.payment.mock.enabled:false}") boolean mockPaymentEnabled,
                          @Value("${app.payment.mock-signature:}") String mockPaymentSignature,
                          @Value("${app.payment.provider:disabled}") String configuredPaymentProvider,
                          Clock clock) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.emailService = emailService;
        this.paymentProviderRegistry = paymentProviderRegistry;
        this.mockPaymentEnabled = mockPaymentEnabled;
        this.mockPaymentSignature = mockPaymentSignature;
        this.configuredPaymentProvider = configuredPaymentProvider;
        this.clock = clock;
    }

    @Transactional
    public PaymentIntent createPaymentIntent(Booking booking) {
        return createPaymentIntent(booking, configuredPaymentProvider, "127.0.0.1");
    }

    @Transactional
    public PaymentIntent createConfiguredPaymentIntent(Booking booking, String clientIpAddress) {
        return createPaymentIntent(booking, configuredPaymentProvider, clientIpAddress);
    }

    @Transactional
    public Payment startMockPayment(Booking booking) {
        assertMockPaymentEnabled();
        PaymentIntent intent = createPaymentIntent(booking, "mock");
        return paymentRepository.findByOrderId(intent.orderId())
                .orElseThrow(() -> new BusinessException("Payment transaction not found"));
    }

    @Transactional
    public PaymentIntent createPaymentIntent(Booking booking, String providerName) {
        return createPaymentIntent(booking, providerName, "127.0.0.1");
    }

    @Transactional
    public PaymentIntent createPaymentIntent(Booking booking, String providerName, String clientIpAddress) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("Booking is not waiting for payment");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now(clock))) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BusinessException("Booking hold has expired");
        }

        PaymentProvider provider = paymentProviderRegistry.require(providerName);
        String idempotencyKey = booking.getId() + ":" + provider.getProviderName();
        Optional<Payment> existing = paymentRepository.findFirstByBookingOrderByCreatedAtDesc(booking);
        if (existing.isPresent()
                && existing.get().getProvider().equalsIgnoreCase(provider.getProviderName())
                && existing.get().getStatus() == PaymentStatus.PENDING) {
            return provider.createPaymentIntent(toIntentRequest(booking, existing.get().getIdempotencyKey(), clientIpAddress));
        }

        PaymentIntent intent = provider.createPaymentIntent(toIntentRequest(booking, idempotencyKey, clientIpAddress));
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider(provider.getProviderName().toUpperCase());
        payment.setOrderId(intent.orderId());
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("VND");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        paymentRepository.save(payment);
        return intent;
    }

    @Transactional(readOnly = true)
    public Payment requirePaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("Payment transaction not found"));
    }

    @Transactional
    public Payment completeLocalMockPayment(String orderId, boolean success) {
        assertMockPaymentEnabled();
        return mockCallback(orderId, success, mockPaymentSignature);
    }

    @Transactional
    public Payment mockCallback(String orderId, boolean success, String signature) {
        assertMockPaymentEnabled();
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("Payment transaction not found"));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment;
        }
        if (mockPaymentSignature.isBlank() || !mockPaymentSignature.equals(signature)) {
            payment.setSignatureValid(false);
            paymentRepository.save(payment);
            throw new BusinessException("Invalid payment signature");
        }
        payment.setSignatureValid(true);
        if (success) {
            markPaid(payment, "MOCK-" + payment.getOrderId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProcessedAt(Instant.now(clock));
        }
        return paymentRepository.save(payment);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Payment handleWebhook(String providerName, Map<String, String> headers, String rawPayload) {
        return handleWebhookDetailed(providerName, headers, rawPayload).payment();
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentWebhookResult handleWebhookDetailed(String providerName, Map<String, String> headers, String rawPayload) {
        PaymentProvider provider = paymentProviderRegistry.require(providerName);
        boolean signatureValid = provider.verifyWebhook(headers, rawPayload);
        PaymentWebhookPayload payload = signatureValid
                ? provider.parseWebhook(headers, rawPayload)
                : invalidPayload(provider.getProviderName(), rawPayload);

        Optional<PaymentWebhookEvent> duplicate = webhookEventRepository.findByProviderEventId(payload.providerEventId());
        if (duplicate.isPresent()) {
            Payment duplicatePayment = paymentRepository.findByOrderId(payload.orderId())
                    .orElseThrow(() -> new BusinessException("Duplicate webhook references an unknown payment"));
            return new PaymentWebhookResult(duplicatePayment, true);
        }

        Payment payment = paymentRepository.findByOrderId(payload.orderId()).orElse(null);
        boolean alreadyProcessed = payment != null && payment.getStatus() != PaymentStatus.PENDING;
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider.getProviderName().toUpperCase());
        event.setProviderEventId(payload.providerEventId());
        event.setPayment(payment);
        event.setOrderId(payload.orderId());
        event.setEventStatus(payload.status());
        event.setSignatureValid(signatureValid);
        event.setRawPayload(jsonEnvelope(rawPayload));

        try {
            if (!signatureValid) {
                throw new BusinessException("Invalid payment webhook signature");
            }
            if (payment == null) {
                throw new BusinessException("Payment transaction not found");
            }
            validateWebhookMatchesPayment(payment, payload);
            applyWebhookStatus(payment, payload);
            event.setProcessed(true);
            event.setProcessedAt(Instant.now(clock));
            return new PaymentWebhookResult(paymentRepository.save(payment), alreadyProcessed);
        } catch (RuntimeException ex) {
            event.setProcessed(false);
            event.setErrorMessage(ex.getMessage());
            throw ex;
        } finally {
            webhookEventRepository.save(event);
        }
    }

    @Transactional
    public RefundRequest submitRefund(RefundRequest refund) {
        Payment payment = refund.getPayment();
        if (payment == null || refund.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            refund.setStatus(RefundStatus.SUCCEEDED);
            refund.setProcessedAt(Instant.now(clock));
            return refundRequestRepository.save(refund);
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException("Only paid payments can be refunded");
        }
        PaymentProvider provider = paymentProviderRegistry.require(payment.getProvider());
        RefundResult result = provider.refund(new RefundCommand(
                refund.getId(),
                payment.getOrderId(),
                payment.getProviderTransactionId(),
                refund.getAmount(),
                payment.getCurrency(),
                refund.getReason(),
                refund.getIdempotencyKey()
        ));
        if (!result.submitted()) {
            refund.setStatus(RefundStatus.FAILED);
            return refundRequestRepository.save(refund);
        }
        if (result.completed()) {
            refund.setStatus(RefundStatus.SUCCEEDED);
            refund.setProcessedAt(Instant.now(clock));
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setProcessedAt(Instant.now(clock));
            refund.getBooking().setStatus(BookingStatus.REFUNDED);
        } else {
            refund.setStatus(RefundStatus.PROCESSING);
            payment.setStatus(PaymentStatus.REFUND_PENDING);
        }
        paymentRepository.save(payment);
        return refundRequestRepository.save(refund);
    }

    private PaymentIntentRequest toIntentRequest(Booking booking, String idempotencyKey, String clientIpAddress) {
        return new PaymentIntentRequest(booking.getId(), booking.getBookingCode(), booking.getTotalAmount(), "VND",
                idempotencyKey, clientIpAddress == null || clientIpAddress.isBlank() ? "127.0.0.1" : clientIpAddress);
    }

    private void applyWebhookStatus(Payment payment, PaymentWebhookPayload payload) {
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        if (payload.status() == PaymentStatus.PAID) {
            markPaid(payment, payload.providerTransactionId());
            return;
        }
        if (payload.status() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProcessedAt(Instant.now(clock));
            return;
        }
        if (payload.status() == PaymentStatus.CANCELLED) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setProcessedAt(Instant.now(clock));
        }
    }

    private void markPaid(Payment payment, String providerTransactionId) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderTransactionId(providerTransactionId);
        payment.setProcessedAt(Instant.now(clock));
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        emailService.enqueue(booking.getUser(), booking, EmailEventType.BOOKING_CONFIRMED,
                booking.getUser().getEmail(), "Booking confirmed " + booking.getBookingCode(), "booking-confirmed");
    }

    private void validateWebhookMatchesPayment(Payment payment, PaymentWebhookPayload payload) {
        if (payload.bookingId() != null && !payload.bookingId().equals(payment.getBooking().getId())) {
            throw new BusinessException("Payment webhook booking mismatch");
        }
        if (payment.getAmount().compareTo(payload.amount()) != 0) {
            throw new BusinessException("Payment webhook amount mismatch");
        }
        if (!payment.getCurrency().equalsIgnoreCase(payload.currency())) {
            throw new BusinessException("Payment webhook currency mismatch");
        }
    }

    private PaymentWebhookPayload invalidPayload(String providerName, String rawPayload) {
        String eventId = "invalid:" + providerName + ":" + sha256(rawPayload == null ? "" : rawPayload);
        return new PaymentWebhookPayload(
                providerName,
                eventId,
                "",
                null,
                BigDecimal.ZERO,
                "VND",
                PaymentStatus.FAILED,
                null,
                rawPayload
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }

    private String jsonEnvelope(String rawPayload) {
        String value = rawPayload == null ? "" : rawPayload;
        return "{\"body\":\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"}";
    }

    private void assertMockPaymentEnabled() {
        if (!mockPaymentEnabled) {
            throw new BusinessException("Mock payment is disabled");
        }
    }
}
