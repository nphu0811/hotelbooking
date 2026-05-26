package com.example.demo.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class BrevoEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(BrevoEmailProvider.class);
    private static final URI BREVO_SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final HttpClient httpClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailProvider(@Value("${brevo.api-key:}") String apiKey,
                              @Value("${mail.from:}") String fromEmail,
                              @Value("${brevo.email.from-name:HotelBooking}") String fromName) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.fromName = fromName == null ? "HotelBooking" : fromName.trim();
    }

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        if (apiKey.isBlank()) {
            return new EmailSendResult(false, null, "BREVO_API_KEY is not configured");
        }
        if (fromEmail.isBlank()) {
            return new EmailSendResult(false, null, "MAIL_FROM is not configured");
        }

        String bodyText = renderBody(request);
        String jsonBody = buildJsonBody(request.recipient(), request.subject(), bodyText);

        HttpRequest httpRequest = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
                .timeout(Duration.ofSeconds(15))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String messageId = extractMessageId(response.body());
                return new EmailSendResult(true, messageId != null ? messageId : "BREVO-" + request.jobId(), null);
            }
            log.warn("Brevo email send failed with status {}: {}", response.statusCode(), response.body());
            return new EmailSendResult(false, null, "Brevo API returned status " + response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new EmailSendResult(false, null, "Brevo email send interrupted");
        } catch (Exception ex) {
            log.warn("Brevo email send failed: {}", ex.getMessage());
            return new EmailSendResult(false, null, "Brevo email send failed: " + ex.getClass().getSimpleName());
        }
    }

    @Override
    public String getProviderName() {
        return "brevo";
    }

    private String buildJsonBody(String to, String subject, String bodyText) {
        return "{"
                + "\"sender\":{\"name\":\"" + jsonEscape(fromName) + "\",\"email\":\"" + jsonEscape(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + jsonEscape(to) + "\"}],"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"textContent\":\"" + jsonEscape(bodyText) + "\""
                + "}";
    }

    private String renderBody(EmailSendRequest request) {
        if (request.bodyText() != null && !request.bodyText().isBlank()) {
            return request.bodyText();
        }
        return switch (request.eventType()) {
            case BOOKING_CONFIRMED -> "Your HotelBooking reservation is confirmed. Sign in to view booking details.";
            case BOOKING_CANCELLED -> "Your HotelBooking reservation cancellation has been recorded.";
            case CHECKED_IN -> "Welcome. Your HotelBooking check-in has been recorded.";
            case CHECKED_OUT -> "Your HotelBooking check-out has been recorded.";
            case REVIEW_REQUEST -> "Thank you for staying with us. Sign in to leave your review.";
            case ACCOUNT_UNLOCKED -> "Your HotelBooking account has been unlocked.";
            case EMAIL_VERIFICATION -> "Verify your HotelBooking account from the latest verification email.";
            case LOGIN_OTP -> "Use the latest OTP email to sign in to HotelBooking.";
        };
    }

    private String extractMessageId(String responseBody) {
        // Simple extraction of messageId from Brevo JSON response
        // Response format: {"messageId":"<xxx@xxx>"}
        int idx = responseBody.indexOf("\"messageId\"");
        if (idx < 0) return null;
        int start = responseBody.indexOf('"', idx + 11);
        if (start < 0) return null;
        start++;
        int end = responseBody.indexOf('"', start);
        if (end < 0) return null;
        return responseBody.substring(start, end);
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.toString();
    }
}
