package com.example.demo.payment;

import java.util.Map;

public interface PaymentProvider {
    PaymentIntent createPaymentIntent(PaymentIntentRequest request);

    boolean verifyWebhook(Map<String, String> headers, String rawPayload);

    PaymentWebhookPayload parseWebhook(Map<String, String> headers, String rawPayload);

    RefundResult refund(RefundCommand command);

    String getProviderName();
}
