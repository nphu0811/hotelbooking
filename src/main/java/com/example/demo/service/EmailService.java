package com.example.demo.service;

import com.example.demo.entity.Booking;
import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.EmailJob;
import com.example.demo.entity.EmailLog;
import com.example.demo.entity.EmailStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.EmailJobRepository;
import com.example.demo.repository.EmailLogRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EmailService {
    private final EmailJobRepository emailJobRepository;
    private final EmailLogRepository emailLogRepository;

    public EmailService(EmailJobRepository emailJobRepository, EmailLogRepository emailLogRepository) {
        this.emailJobRepository = emailJobRepository;
        this.emailLogRepository = emailLogRepository;
    }

    @Transactional
    public EmailJob enqueue(User user, Booking booking, EmailEventType eventType,
                            String recipient, String subject, String templateName) {
        EmailJob job = new EmailJob();
        job.setUser(user);
        job.setBooking(booking);
        job.setEventType(eventType);
        job.setRecipient(recipient);
        job.setSubject(subject);
        job.setTemplateName(templateName);
        return emailJobRepository.save(job);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processPendingEmails() {
        Instant now = Instant.now();
        for (EmailJob job : emailJobRepository.findTop20ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(EmailStatus.PENDING, now)) {
            job.setAttempts(job.getAttempts() + 1);
            job.setStatus(EmailStatus.SENT);
            EmailLog log = new EmailLog();
            log.setJob(job);
            log.setBooking(job.getBooking());
            log.setEventType(job.getEventType());
            log.setRecipient(job.getRecipient());
            log.setStatus(EmailStatus.SENT);
            log.setSentAt(now);
            emailLogRepository.save(log);
            emailJobRepository.save(job);
        }
    }
}
