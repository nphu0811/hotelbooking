package com.example.demo.service;

import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Duration VERIFICATION_RESEND_COOLDOWN = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;
    private final String publicBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       Clock clock,
                       @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.clock = clock;
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    @Transactional
    public User register(String fullName, String email, String phone, String password, String confirmPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new BusinessException("Full name must be 2 to 100 characters");
        }
        if (!normalizedEmail.contains("@")) {
            throw new BusinessException("Email is invalid");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException("Vietnam phone number is invalid");
        }
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException("Password needs at least 8 characters, 1 uppercase letter, 1 number and 1 special character");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Password confirmation does not match");
        }

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION) {
                resendVerificationIfAllowed(existing);
            }
            return existing;
        }

        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new BusinessException("Missing USER role"));
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        String rawToken = setVerificationToken(user, Instant.now(clock));
        user.getRoles().add(userRole);
        User saved = userRepository.save(user);
        enqueueVerificationEmail(saved, rawToken);
        return saved;
    }

    @Transactional
    public void verify(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(hashToken(token))
                .orElseThrow(() -> new BusinessException("Verification link is invalid"));
        if (user.getEmailVerificationExpiresAt() == null
                || user.getEmailVerificationExpiresAt().isBefore(Instant.now(clock))) {
            throw new BusinessException("Verification link has expired");
        }
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setEmailVerificationLastSentAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(this::resendVerificationIfAllowed);
    }

    private void resendVerificationIfAllowed(User user) {
        Instant now = Instant.now(clock);
        if (user.getEmailVerificationLastSentAt() != null
                && user.getEmailVerificationLastSentAt().plus(VERIFICATION_RESEND_COOLDOWN).isAfter(now)) {
            return;
        }
        String rawToken = setVerificationToken(user, now);
        userRepository.save(user);
        enqueueVerificationEmail(user, rawToken);
    }

    private String setVerificationToken(User user, Instant now) {
        String rawToken = generateVerificationToken();
        user.setEmailVerificationTokenHash(hashToken(rawToken));
        user.setEmailVerificationExpiresAt(now.plus(24, ChronoUnit.HOURS));
        user.setEmailVerificationLastSentAt(now);
        return rawToken;
    }

    private void enqueueVerificationEmail(User user, String rawToken) {
        String verifyUrl = publicBaseUrl + "/verify/" + rawToken;
        String body = "Welcome to HotelBooking.\n\n"
                + "Verify your email address by opening this link within 24 hours:\n"
                + verifyUrl + "\n\n"
                + "If you did not create this account, ignore this email.";
        emailService.enqueue(user, null, EmailEventType.EMAIL_VERIFICATION,
                user.getEmail(), "Verify your HotelBooking account", "email-verification", body);
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Verification link is invalid");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("Verification token could not be checked");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String authority = uri.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            return "http://localhost:8080";
        }
        return scheme + "://" + authority + (uri.getRawPath() == null ? "" : uri.getRawPath()).replaceAll("/+$", "");
    }
}
