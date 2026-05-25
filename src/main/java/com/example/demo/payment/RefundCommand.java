package com.example.demo.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundCommand(
        UUID refundId,
        String orderId,
        String providerTransactionId,
        BigDecimal amount,
        String currency,
        String reason,
        String idempotencyKey
) {
}
