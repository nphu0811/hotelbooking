package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updateProfile(User user, String fullName, String phone, String currentPassword, String newPassword, String confirmPassword) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new BusinessException("Họ tên phải từ 2 đến 100 ký tự");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException("Số điện thoại không hợp lệ");
        }

        user.setFullName(fullName.trim());
        user.setPhone(phone.trim());

        // Optional password change
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new BusinessException("Vui lòng nhập mật khẩu hiện tại để đổi mật khẩu");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new BusinessException("Mật khẩu hiện tại không chính xác");
            }
            if (!STRONG_PASSWORD.matcher(newPassword).matches()) {
                throw new BusinessException("Mật khẩu mới cần tối thiểu 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new BusinessException("Xác nhận mật khẩu mới không khớp");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
    }
}
