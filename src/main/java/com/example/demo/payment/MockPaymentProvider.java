package com.example.demo.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
public class MockPaymentProvider implements PaymentProvider {
    @Value("${app.payment.mock-signature:}")
    private String mockSignature;

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) {
        String orderId = "MOCK-" + request.bookingId();
        String redirectUrl = "/payments/mock/" + orderId;
        String payload = "{\"provider\":\"mock\",\"bookingId\":\"" + request.bookingId() + "\"}";
        return new PaymentIntent(getProviderName(), orderId, redirectUrl, payload);
    }

    @Override
    public boolean verifyWebhook(java.util.Map<String, String> headers, String rawPayload) {
        return mockSignature != null && !mockSignature.isBlank();
    }

    @Override
    public PaymentWebhookPayload parseWebhook(java.util.Map<String, String> headers, String rawPayload) {
        throw new UnsupportedOperationException("Mock webhook parsing is handled by local test controller");
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        return new RefundResult(true, true, "MOCK-REFUND-" + command.refundId(), "Local mock refund completed");
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
