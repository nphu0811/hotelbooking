package com.example.demo.repository;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findFirstByBookingOrderByCreatedAtDesc(Booking booking);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant createdAt);
}
