package com.example.demo.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class SmtpEmailProvider implements EmailProvider {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public SmtpEmailProvider(@Value("${mail.host:}") String host,
                             @Value("${mail.port:587}") int port,
                             @Value("${mail.username:}") String username,
                             @Value("${mail.password:}") String password,
                             @Value("${mail.from:}") String from) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
    }

    @Override
    public EmailSendResult send(EmailSendRequest request) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            sender.setUsername(username);
            sender.setPassword(password);
            Properties properties = sender.getJavaMailProperties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(request.recipient());
            message.setSubject(request.subject());
            message.setText(renderBody(request));
            sender.send(message);
            return new EmailSendResult(true, "SMTP-" + request.jobId(), null);
        } catch (RuntimeException ex) {
            return new EmailSendResult(false, null, sanitize(ex.getMessage()));
        }
    }

    @Override
    public String getProviderName() {
        return "smtp";
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "SMTP send failed";
        }
        if (password == null || password.isBlank()) {
            return message;
        }
        return message.replace(password, "[redacted]");
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
        };
    }
}
