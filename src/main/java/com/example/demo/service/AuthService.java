package com.example.demo.service;

import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public User register(String fullName, String email, String phone, String password, String confirmPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new BusinessException("Họ tên phải từ 2 đến 100 ký tự");
        }
        if (!normalizedEmail.contains("@")) {
            throw new BusinessException("Email không hợp lệ");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException("Số điện thoại Việt Nam không hợp lệ");
        }
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException("Mật khẩu cần ít nhất 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Xác nhận mật khẩu không khớp");
        }
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION) {
                resendVerification(existing);
                return existing;
            }
            throw new BusinessException("Email đã được đăng ký");
        }

        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new BusinessException("Thiếu role USER"));
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(phone.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerificationToken(UUID.randomUUID());
        user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        user.getRoles().add(userRole);
        User saved = userRepository.save(user);
        emailService.enqueue(saved, null, EmailEventType.EMAIL_VERIFICATION,
                saved.getEmail(), "Xác minh tài khoản HotelBooking", "email-verification");
        return saved;
    }

    @Transactional
    public void verify(UUID token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BusinessException("Link xác minh không hợp lệ"));
        if (user.getEmailVerificationExpiresAt() == null || user.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Link xác minh đã hết hạn");
        }
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(User user) {
        user.setEmailVerificationToken(UUID.randomUUID());
        user.setEmailVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        userRepository.save(user);
        emailService.enqueue(user, null, EmailEventType.EMAIL_VERIFICATION,
                user.getEmail(), "Xác minh tài khoản HotelBooking", "email-verification");
    }
}
