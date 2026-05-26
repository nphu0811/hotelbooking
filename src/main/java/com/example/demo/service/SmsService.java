package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final URI BREVO_SEND_SMS_URI = URI.create("https://api.brevo.com/v3/transactionalSMS/send");

    private final HttpClient httpClient;
    private final String providerName;
    private final String brevoApiKey;
    private final String brevoSender;

    public SmsService(@Value("${app.sms.provider:disabled}") String providerName,
                      @Value("${brevo.api-key:}") String brevoApiKey,
                      @Value("${brevo.sms.sender:HotelBook}") String brevoSender) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.providerName = providerName == null ? "disabled" : providerName.trim();
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.brevoSender = brevoSender == null || brevoSender.isBlank() ? "HotelBook" : brevoSender.trim();
    }

    public void sendOtp(String phone, String content) {
        if ("console".equalsIgnoreCase(providerName)) {
            log.info("SMS OTP to {}: {}", maskPhone(phone), content);
            return;
        }
        if (!"brevo".equalsIgnoreCase(providerName)) {
            throw new BusinessException("SMS provider chưa được cấu hình. Vui lòng bật APP_SMS_PROVIDER=brevo hoặc console.");
        }
        if (brevoApiKey.isBlank()) {
            throw new BusinessException("BREVO_API_KEY chưa được cấu hình.");
        }

        String body = "{"
                + "\"sender\":\"" + jsonEscape(limitSender(brevoSender)) + "\","
                + "\"recipient\":\"" + jsonEscape(toBrevoRecipient(phone)) + "\","
                + "\"content\":\"" + jsonEscape(content) + "\","
                + "\"type\":\"transactional\","
                + "\"tag\":\"hotelbooking-otp\","
                + "\"unicodeEnabled\":true"
                + "}";
        HttpRequest request = HttpRequest.newBuilder(BREVO_SEND_SMS_URI)
                .timeout(Duration.ofSeconds(15))
                .header("accept", "application/json")
                .header("api-key", brevoApiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.info("Brevo SMS response status: {}, body: {}", response.statusCode(), response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }
            log.warn("Brevo SMS send failed with status {}", response.statusCode());
            throw new BusinessException("Không gửi được SMS OTP. Vui lòng thử lại sau.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Không gửi được SMS OTP. Vui lòng thử lại sau.");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Brevo SMS send failed: {}", ex.getClass().getSimpleName());
            throw new BusinessException("Không gửi được SMS OTP. Vui lòng thử lại sau.");
        }
    }

    private String toBrevoRecipient(String phone) {
        String normalized = phone == null ? "" : phone.trim().replaceAll("[\\s.-]", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0")) {
            normalized = "84" + normalized.substring(1);
        }
        return normalized;
    }

    private String limitSender(String sender) {
        return sender.length() <= 11 ? sender : sender.substring(0, 11);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    private String jsonEscape(String value) {
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
