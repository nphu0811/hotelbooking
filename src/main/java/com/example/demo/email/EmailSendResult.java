package com.example.demo.email;

public record EmailSendResult(
        boolean sent,
        String providerMessageId,
        String errorMessage
) {
}
