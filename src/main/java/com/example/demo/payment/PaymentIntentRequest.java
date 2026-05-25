package com.example.demo.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentIntentRequest(
        UUID bookingId,
        String bookingCode,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String clientIpAddress
) {
}
