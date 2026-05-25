package com.example.demo.payment;

public record PaymentIntent(
        String provider,
        String orderId,
        String redirectUrl,
        String requestPayload
) {
}
