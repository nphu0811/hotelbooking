package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User updateProfile(User currentUser,
                              String fullName,
                              String email,
                              String phone,
                              String currentPassword,
                              String newPassword,
                              String confirmPassword) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản hiện tại."));
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        validateFullName(fullName);
        validateEmail(normalizedEmail);
        validatePhone(normalizedPhone);

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Email đã được sử dụng bởi tài khoản khác.");
                });
        boolean phoneUsedByAnother = userRepository.findByPhone(normalizedPhone).stream()
                .anyMatch(existing -> !existing.getId().equals(user.getId()));
        if (phoneUsedByAnother) {
            throw new BusinessException("Số điện thoại đã được sử dụng bởi tài khoản khác.");
        }

        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            user.setEmail(normalizedEmail);
            user.setEmailVerified(false);
            user.setEmailVerificationTokenHash(null);
            user.setEmailVerificationExpiresAt(null);
            user.setEmailVerificationLastSentAt(null);
        }
        if (!normalizedPhone.equals(user.getPhone())) {
            user.setPhone(normalizedPhone);
            user.setPhoneVerified(false);
            user.setPhoneVerificationTokenHash(null);
            user.setPhoneVerificationExpiresAt(null);
            user.setPhoneVerificationLastSentAt(null);
        }
        user.setFullName(fullName.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new BusinessException("Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu.");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new BusinessException("Mật khẩu hiện tại không chính xác.");
            }
            if (!STRONG_PASSWORD.matcher(newPassword).matches()) {
                throw new BusinessException("Mật khẩu mới cần tối thiểu 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt.");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new BusinessException("Họ tên phải từ 2 đến 100 ký tự.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("Định dạng email không hợp lệ.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("Số điện thoại Việt Nam không hợp lệ.");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim().replaceAll("[\\s.-]", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        }
        return normalized;
    }
}
