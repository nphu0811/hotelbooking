package com.example.demo.payment;

import com.example.demo.entity.Payment;

public record PaymentWebhookResult(Payment payment, boolean alreadyProcessed) {
}
