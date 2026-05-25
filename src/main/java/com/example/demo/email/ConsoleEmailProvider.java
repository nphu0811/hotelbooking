package com.example.demo.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
public class ConsoleEmailProvider implements EmailProvider {
    @Override
    public EmailSendResult send(EmailSendRequest request) {
        return new EmailSendResult(true, "CONSOLE-" + request.jobId(), null);
    }

    @Override
    public String getProviderName() {
        return "console";
    }
}
