package com.example.demo.email;

public interface EmailProvider {
    EmailSendResult send(EmailSendRequest request);

    String getProviderName();
}
