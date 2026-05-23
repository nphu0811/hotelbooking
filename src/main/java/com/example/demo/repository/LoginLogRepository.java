package com.example.demo.repository;

import com.example.demo.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginLogRepository extends JpaRepository<LoginLog, UUID> {
}
