package com.example.demo.email;

import com.example.demo.entity.EmailEventType;

import java.util.UUID;

public record EmailSendRequest(
        UUID jobId,
        EmailEventType eventType,
        String recipient,
        String subject,
        String templateName,
        String bodyText
) {
}
