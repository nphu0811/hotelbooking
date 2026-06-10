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
    private final String appUrl;

    public BrevoEmailProvider(@Value("${brevo.api-key:}") String apiKey,
                              @Value("${mail.from:}") String fromEmail,
                              @Value("${brevo.email.from-name:HotelBooking}") String fromName,
                              @Value("${app.base-url:https://hotelbooking-production-57a9.up.railway.app}") String appUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.fromName = fromName == null || fromName.isBlank() ? "HotelBooking" : fromName.trim();
        this.appUrl = appUrl == null || appUrl.isBlank()
                ? "https://hotelbooking-production-57a9.up.railway.app"
                : appUrl.trim();
    }

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        if (apiKey.isBlank()) {
            return new EmailSendResult(false, null, "BREVO_API_KEY is not configured");
        }
        if (fromEmail.isBlank()) {
            return new EmailSendResult(false, null, "MAIL_FROM is not configured");
        }

        EmailBody emailBody = renderBody(request);
        String jsonBody = buildJsonBody(
                request.recipient(),
                request.subject(),
                emailBody.htmlContent(),
                emailBody.textContent()
        );

        HttpRequest httpRequest = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
                .timeout(Duration.ofSeconds(15))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String messageId = extractMessageId(response.body());
                return new EmailSendResult(
                        true,
                        messageId != null ? messageId : "BREVO-" + request.jobId(),
                        null
                );
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

    private String buildJsonBody(String to, String subject, String htmlContent, String textContent) {
        return "{"
                + "\"sender\":{\"name\":\"" + jsonEscape(fromName) + "\",\"email\":\"" + jsonEscape(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + jsonEscape(to) + "\"}],"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"htmlContent\":\"" + jsonEscape(htmlContent) + "\","
                + "\"textContent\":\"" + jsonEscape(textContent) + "\""
                + "}";
    }

    private EmailBody renderBody(EmailSendRequest request) {
        String customBody = request.bodyText();

        if (customBody != null && !customBody.isBlank()) {
            String title = "Notification from HotelBooking";
            String message = customBody.trim();

            return new EmailBody(
                    buildHtmlTemplate(
                            title,
                            escapeHtml(message).replace("\n", "<br>"),
                            "View details",
                            appUrl
                    ),
                    title + "\n\n" + message + "\n\nView details: " + appUrl
            );
        }

        EmailContent content = switch (request.eventType()) {
            case BOOKING_CONFIRMED -> new EmailContent(
                    "Reservation confirmed",
                    "Your HotelBooking reservation has been confirmed successfully. You can sign in to view your booking details, payment status, and stay information.",
                    "View booking",
                    appUrl + "/bookings"
            );

            case BOOKING_CANCELLED -> new EmailContent(
                    "Reservation cancelled",
                    "Your reservation cancellation has been recorded. If your booking is eligible for a refund, the refund process will be handled according to our policy.",
                    "View booking history",
                    appUrl + "/bookings"
            );

            case CHECKED_IN -> new EmailContent(
                    "Check-in completed",
                    "Welcome to HotelBooking. Your check-in has been recorded successfully. We hope you enjoy your stay.",
                    "View stay details",
                    appUrl + "/bookings"
            );

            case CHECKED_OUT -> new EmailContent(
                    "Check-out completed",
                    "Your check-out has been recorded successfully. Thank you for choosing HotelBooking. We hope to welcome you again soon.",
                    "View booking history",
                    appUrl + "/bookings"
            );

            case REVIEW_REQUEST -> new EmailContent(
                    "How was your stay?",
                    "Thank you for staying with us. Your feedback helps us improve our service and helps other guests choose the right hotel.",
                    "Leave a review",
                    appUrl + "/bookings"
            );

            case ACCOUNT_UNLOCKED -> new EmailContent(
                    "Account unlocked",
                    "Your HotelBooking account has been unlocked. You can now sign in and continue using your account normally.",
                    "Sign in",
                    appUrl + "/login"
            );

            case EMAIL_VERIFICATION -> new EmailContent(
                    "Verify your email address",
                    "Please verify your HotelBooking account using the latest verification email. If you did not create this account, you can safely ignore this message.",
                    "Go to HotelBooking",
                    appUrl
            );

            case LOGIN_OTP -> new EmailContent(
                    "Your login OTP",
                    "Please use the latest OTP email to sign in to your HotelBooking account. For your security, do not share this code with anyone.",
                    "Sign in",
                    appUrl + "/login"
            );
        };

        String html = buildHtmlTemplate(
                content.title(),
                escapeHtml(content.message()),
                content.buttonText(),
                content.buttonUrl()
        );

        String text = content.title()
                + "\n\n"
                + content.message()
                + "\n\n"
                + content.buttonText() + ": " + content.buttonUrl();

        return new EmailBody(html, text);
    }

    private String buildHtmlTemplate(String title, String messageHtml, String buttonText, String buttonUrl) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#111827;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:32px 12px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                       style="max-width:620px;background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 16px 40px rgba(15,23,42,0.12);">
                
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#111827 0%%,#1f2937 45%%,#dc2626 100%%);padding:30px 34px;">
                                            <div style="font-size:13px;letter-spacing:1.8px;text-transform:uppercase;color:#fecaca;font-weight:700;">
                                                HotelBooking
                                            </div>
                                            <h1 style="margin:12px 0 0;font-size:28px;line-height:1.3;color:#ffffff;font-weight:800;">
                                                %s
                                            </h1>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:34px;">
                                            <p style="margin:0 0 18px;font-size:16px;line-height:1.7;color:#374151;">
                                                Hello,
                                            </p>
                
                                            <p style="margin:0 0 28px;font-size:16px;line-height:1.7;color:#374151;">
                                                %s
                                            </p>
                
                                            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 30px;">
                                                <tr>
                                                    <td>
                                                        <a href="%s"
                                                           style="display:inline-block;background:#dc2626;color:#ffffff;text-decoration:none;
                                                                  font-size:15px;font-weight:700;padding:14px 24px;border-radius:999px;">
                                                            %s
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                
                                            <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:16px;padding:16px 18px;">
                                                <p style="margin:0;font-size:13px;line-height:1.6;color:#6b7280;">
                                                    If the button does not work, please sign in to your HotelBooking account directly from our website.
                                                </p>
                                            </div>
                                        </td>
                                    </tr>
                
                                    <tr>
                                        <td style="padding:22px 34px;background:#111827;">
                                            <p style="margin:0;font-size:13px;line-height:1.6;color:#d1d5db;">
                                                This is an automated email from HotelBooking. Please do not reply to this message.
                                            </p>
                                            <p style="margin:8px 0 0;font-size:12px;color:#9ca3af;">
                                                © HotelBooking. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(title),
                messageHtml,
                escapeHtml(buttonUrl),
                escapeHtml(buttonText)
        );
    }

    private String extractMessageId(String responseBody) {
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

    private String escapeHtml(String value) {
        if (value == null) return "";

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record EmailContent(
            String title,
            String message,
            String buttonText,
            String buttonUrl
    ) {
    }

    private record EmailBody(
            String htmlContent,
            String textContent
    ) {
    }
}