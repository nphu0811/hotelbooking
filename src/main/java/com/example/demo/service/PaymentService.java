package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.BookingStatus;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Payment startMockPayment(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("Đơn không ở trạng thái chờ thanh toán");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BusinessException("Phiên đặt phòng đã hết hạn");
        }
        Optional<Payment> existing = paymentRepository.findFirstByBookingOrderByCreatedAtDesc(booking);
        if (existing.isPresent()) {
            return existing.get();
        }
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider("MOCK");
        payment.setOrderId("MOCK-" + UUID.randomUUID());
        payment.setAmount(booking.getTotalAmount());
        payment.setIdempotencyKey(booking.getId() + ":mock");
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment mockCallback(String orderId, boolean success, String signature) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy giao dịch"));
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            return payment;
        }
        if (!"mock-secret".equals(signature)) {
            payment.setSignatureValid(false);
            paymentRepository.save(payment);
            throw new BusinessException("Chữ ký thanh toán không hợp lệ");
        }
        payment.setSignatureValid(true);
        payment.setProcessedAt(Instant.now());
        Booking booking = payment.getBooking();
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            emailService.enqueue(booking.getUser(), booking, EmailEventType.BOOKING_CONFIRMED,
                    booking.getUser().getEmail(), "Xác nhận đặt phòng " + booking.getBookingCode(), "booking-confirmed");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        return paymentRepository.save(payment);
    }
}
