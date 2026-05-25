package com.example.demo.service;

import com.example.demo.email.EmailProvider;
import com.example.demo.email.EmailProviderRegistry;
import com.example.demo.email.EmailSendRequest;
import com.example.demo.email.EmailSendResult;
import com.example.demo.entity.Booking;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.EmailJob;
import com.example.demo.entity.EmailLog;
import com.example.demo.entity.EmailStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.EmailJobRepository;
import com.example.demo.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class EmailService {
    private static final int MAX_ATTEMPTS = 3;

    private final EmailJobRepository emailJobRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailProviderRegistry emailProviderRegistry;
    private final String emailProviderName;
    private final Clock clock;

    public EmailService(EmailJobRepository emailJobRepository,
                        EmailLogRepository emailLogRepository,
                        EmailProviderRegistry emailProviderRegistry,
                        @Value("${app.email.provider:disabled}") String emailProviderName,
                        Clock clock) {
        this.emailJobRepository = emailJobRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailProviderRegistry = emailProviderRegistry;
        this.emailProviderName = emailProviderName;
        this.clock = clock;
    }

    @Transactional
    public EmailJob enqueue(User user, Booking booking, EmailEventType eventType,
                            String recipient, String subject, String templateName) {
        return enqueue(user, booking, eventType, recipient, subject, templateName, null);
    }

    @Transactional
    public EmailJob enqueue(User user, Booking booking, EmailEventType eventType,
                            String recipient, String subject, String templateName, String bodyText) {
        EmailJob job = new EmailJob();
        job.setUser(user);
        job.setBooking(booking);
        job.setEventType(eventType);
        job.setRecipient(recipient);
        job.setSubject(subject);
        job.setTemplateName(templateName);
        job.setBodyText(bodyText);
        return emailJobRepository.save(job);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processPendingEmails() {
        Instant now = Instant.now(clock);
        List<EmailStatus> readyStatuses = List.of(EmailStatus.PENDING, EmailStatus.RETRYING);
        EmailProvider provider = emailProviderRegistry.require(emailProviderName);
        for (EmailJob job : emailJobRepository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(readyStatuses, now)) {
            if (job.getAttempts() >= MAX_ATTEMPTS) {
                job.setStatus(EmailStatus.FAILED);
                emailJobRepository.save(job);
                continue;
            }
            job.setAttempts(job.getAttempts() + 1);
            EmailSendResult result = provider.send(new EmailSendRequest(
                    job.getId(),
                    job.getEventType(),
                    job.getRecipient(),
                    job.getSubject(),
                    job.getTemplateName(),
                    job.getBodyText()
            ));

            EmailLog log = new EmailLog();
            log.setJob(job);
            log.setBooking(job.getBooking());
            log.setEventType(job.getEventType());
            log.setRecipient(job.getRecipient());
            log.setProviderMessageId(result.providerMessageId());

            if (result.sent()) {
                job.setStatus(EmailStatus.SENT);
                job.setProviderMessageId(result.providerMessageId());
                job.setLastError(null);
                log.setStatus(EmailStatus.SENT);
                log.setSentAt(now);
            } else {
                job.setLastError(result.errorMessage());
                if (job.getAttempts() >= MAX_ATTEMPTS) {
                    job.setStatus(EmailStatus.FAILED);
                } else {
                    job.setStatus(EmailStatus.RETRYING);
                    job.setNextAttemptAt(now.plus(backoff(job.getAttempts())));
                }
                log.setStatus(EmailStatus.FAILED);
                log.setErrorMessage(result.errorMessage());
            }

            emailLogRepository.save(log);
            emailJobRepository.save(job);
        }
    }

    private Duration backoff(int attempts) {
        return Duration.ofMinutes((long) Math.pow(2, Math.max(1, attempts)));
    }
}
