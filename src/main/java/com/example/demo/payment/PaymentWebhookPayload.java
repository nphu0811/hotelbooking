package com.example.demo.payment;

import com.example.demo.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentWebhookPayload(
        String provider,
        String providerEventId,
        String orderId,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerTransactionId,
        String rawPayload
) {
}
