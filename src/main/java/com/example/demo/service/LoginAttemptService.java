package com.example.demo.service;

import com.example.demo.entity.LoginLog;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.LoginLogRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class LoginAttemptService {
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int LOCK_THRESHOLD = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;

    public LoginAttemptService(UserRepository userRepository, LoginLogRepository loginLogRepository) {
        this.userRepository = userRepository;
        this.loginLogRepository = loginLogRepository;
    }

    @Transactional
    public void recordSuccess(Authentication authentication, HttpServletRequest request) {
        String email = normalize(authentication.getName());
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        user.ifPresent(value -> {
            value.setFailedLoginCount(0);
            value.setLastFailedLoginAt(null);
            value.setLockedUntil(null);
            value.setLockReason(null);
            userRepository.save(value);
        });
        saveLog(email, user.orElse(null), true, null, request);
    }

    @Transactional
    public LoginFailureResult recordFailure(String submittedEmail,
                                            AuthenticationException exception,
                                            HttpServletRequest request) {
        String email = normalize(submittedEmail);
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        Instant now = Instant.now();
        boolean showCaptcha = false;
        boolean locked = false;

        if (user.isPresent()) {
            User value = user.get();
            if (value.getLockedUntil() != null && value.getLockedUntil().isAfter(now)) {
                locked = true;
                showCaptcha = true;
            } else {
                int failures = value.getLastFailedLoginAt() != null
                        && value.getLastFailedLoginAt().isAfter(now.minus(FAILURE_WINDOW))
                        ? value.getFailedLoginCount() + 1
                        : 1;
                value.setFailedLoginCount(failures);
                value.setLastFailedLoginAt(now);
                showCaptcha = failures >= CAPTCHA_THRESHOLD;
                if (failures >= LOCK_THRESHOLD) {
                    value.setStatus(UserStatus.LOCKED);
                    value.setLockedUntil(now.plus(FAILURE_WINDOW));
                    value.setLockReason("Too many failed login attempts");
                    locked = true;
                }
                userRepository.save(value);
            }
        }

        saveLog(email, user.orElse(null), false, exception.getClass().getSimpleName(), request);
        return new LoginFailureResult(showCaptcha, locked);
    }

    private void saveLog(String email, User user, boolean success, String failureReason, HttpServletRequest request) {
        LoginLog log = new LoginLog();
        log.setEmail(email.isBlank() ? "unknown" : email);
        log.setUser(user);
        log.setSuccess(success);
        log.setFailureReason(failureReason);
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        loginLogRepository.save(log);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginFailureResult(boolean showCaptcha, boolean locked) {
    }
}
