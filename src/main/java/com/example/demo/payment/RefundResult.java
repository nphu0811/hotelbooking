package com.example.demo.payment;

public record RefundResult(
        boolean submitted,
        boolean completed,
        String providerRefundId,
        String message
) {
}
