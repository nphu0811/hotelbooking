package com.example.demo.repository;

import com.example.demo.entity.EmailJob;
import com.example.demo.entity.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailJobRepository extends JpaRepository<EmailJob, UUID> {
    List<EmailJob> findTop20ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(EmailStatus status, Instant now);
}
