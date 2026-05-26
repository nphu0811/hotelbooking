package com.example.demo.service;

import com.example.demo.entity.EmailEventType;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)\\d{9,10}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Duration VERIFICATION_RESEND_COOLDOWN = Duration.ofMinutes(10);
    private static final Duration LOGIN_OTP_RESEND_COOLDOWN = Duration.ofMinutes(2);
    private static final int OTP_EXPIRES_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;
    private final Clock clock;
    private final String publicBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       SmsService smsService,
                       Clock clock,
                       @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
        this.clock = clock;
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    @Transactional
    public User register(String fullName, String email, String phone, String password, String confirmPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        validateFullName(fullName);
        validateEmail(normalizedEmail);
        validatePhone(normalizedPhone);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Xác nhận mật khẩu không khớp.");
        }

        var existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            User existing = existingUser.get();
            if (existing.getStatus() == UserStatus.PENDING_VERIFICATION && !existing.isEmailVerified()) {
                resendVerificationIfAllowed(existing);
                return existing;
            }
            throw new BusinessException("Email đã được sử dụng. Vui lòng đăng nhập hoặc xác thực tài khoản hiện có.");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new BusinessException("Số điện thoại đã được sử dụng.");
        }

        Role userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new BusinessException("Thiếu role USER."));
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        String rawToken = setEmailVerificationToken(user, Instant.now(clock));
        user.getRoles().add(userRole);
        User saved = userRepository.save(user);
        enqueueVerificationEmail(saved, rawToken);
        return saved;
    }

    @Transactional
    public void verify(String token) {
        User user = userRepository.findByEmailVerificationTokenHash(hashToken(token))
                .orElseThrow(() -> new BusinessException("Mã xác thực không hợp lệ."));
        ensureNotExpired(user.getEmailVerificationExpiresAt(), "Mã xác thực đã hết hạn.");
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        clearEmailVerification(user);
        userRepository.save(user);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản người dùng."));
        verifyEmailOtpForUser(user, otp);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(u -> !u.isEmailVerified())
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu xác thực cho tài khoản này."));
        Instant now = Instant.now(clock);
        ensureCooldownElapsed(user.getEmailVerificationLastSentAt(), now, VERIFICATION_RESEND_COOLDOWN,
                "Vui lòng đợi 10 phút trước khi yêu cầu gửi lại mã OTP mới.");
        String rawToken = setEmailVerificationToken(user, now);
        userRepository.save(user);
        enqueueVerificationEmail(user, rawToken);
    }

    @Transactional
    public OtpDelivery requestLoginOtp(String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        boolean emailIdentifier = isEmailIdentifier(normalizedIdentifier);
        User user = findUserByIdentifier(normalizedIdentifier, emailIdentifier);
        ensureCanReceiveLoginOtp(user);

        Instant now = Instant.now(clock);
        ensureCooldownElapsed(user.getLoginOtpLastSentAt(), now, LOGIN_OTP_RESEND_COOLDOWN,
                "Vui lòng đợi 2 phút trước khi yêu cầu mã OTP mới.");
        String rawToken = setLoginOtp(user, now);
        userRepository.save(user);

        if (emailIdentifier) {
            enqueueLoginOtpEmail(user, rawToken);
            return new OtpDelivery(normalizedIdentifier, "email", maskEmail(user.getEmail()));
        }
        smsService.sendOtp(user.getPhone(), "Ma OTP dang nhap HotelBooking cua ban la: " + rawToken
                + ". Ma co hieu luc trong " + OTP_EXPIRES_MINUTES + " phut.");
        return new OtpDelivery(normalizedIdentifier, "phone", maskPhone(user.getPhone()));
    }

    @Transactional
    public User verifyLoginOtp(String identifier, String otp) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        boolean emailIdentifier = isEmailIdentifier(normalizedIdentifier);
        User user = findUserByIdentifier(normalizedIdentifier, emailIdentifier);
        if (user.getLoginOtpTokenHash() == null || !user.getLoginOtpTokenHash().equals(hashToken(otp))) {
            throw new BusinessException("Mã OTP không chính xác, vui lòng kiểm tra lại.");
        }
        ensureNotExpired(user.getLoginOtpExpiresAt(), "Mã OTP đã hết hạn, vui lòng gửi lại mã mới.");
        if (emailIdentifier) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        clearLoginOtp(user);
        return userRepository.save(user);
    }

    @Transactional
    public User requestEmailVerification(User currentUser) {
        User user = reload(currentUser);
        if (user.isEmailVerified()) {
            return user;
        }
        Instant now = Instant.now(clock);
        ensureCooldownElapsed(user.getEmailVerificationLastSentAt(), now, VERIFICATION_RESEND_COOLDOWN,
                "Vui lòng đợi 10 phút trước khi yêu cầu gửi lại mã OTP email.");
        String rawToken = setEmailVerificationToken(user, now);
        User saved = userRepository.save(user);
        enqueueVerificationEmail(saved, rawToken);
        return saved;
    }

    @Transactional
    public User verifyEmailOtp(User currentUser, String otp) {
        return verifyEmailOtpForUser(reload(currentUser), otp);
    }

    @Transactional
    public User requestPhoneVerification(User currentUser) {
        User user = reload(currentUser);
        validatePhone(user.getPhone());
        if (user.isPhoneVerified()) {
            return user;
        }
        Instant now = Instant.now(clock);
        ensureCooldownElapsed(user.getPhoneVerificationLastSentAt(), now, VERIFICATION_RESEND_COOLDOWN,
                "Vui lòng đợi 10 phút trước khi yêu cầu gửi lại mã OTP số điện thoại.");
        String rawToken = setPhoneVerificationToken(user, now);
        userRepository.save(user);
        smsService.sendOtp(user.getPhone(), "Ma OTP xac thuc so dien thoai HotelBooking cua ban la: " + rawToken
                + ". Ma co hieu luc trong " + OTP_EXPIRES_MINUTES + " phut.");
        return user;
    }

    @Transactional
    public User verifyPhoneOtp(User currentUser, String otp) {
        User user = reload(currentUser);
        if (user.getPhoneVerificationTokenHash() == null || !user.getPhoneVerificationTokenHash().equals(hashToken(otp))) {
            throw new BusinessException("Mã OTP số điện thoại không chính xác.");
        }
        ensureNotExpired(user.getPhoneVerificationExpiresAt(), "Mã OTP số điện thoại đã hết hạn.");
        user.setPhoneVerified(true);
        clearPhoneVerification(user);
        return userRepository.save(user);
    }

    public boolean isEmailIdentifier(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    public String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim().replaceAll("[\\s.-]", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        }
        return normalized;
    }

    private User verifyEmailOtpForUser(User user, String otp) {
        if (user.getEmailVerificationTokenHash() == null || !user.getEmailVerificationTokenHash().equals(hashToken(otp))) {
            throw new BusinessException("Mã OTP không chính xác, vui lòng kiểm tra lại.");
        }
        ensureNotExpired(user.getEmailVerificationExpiresAt(), "Mã OTP đã hết hạn, vui lòng gửi lại mã mới.");
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        clearEmailVerification(user);
        return userRepository.save(user);
    }

    private void resendVerificationIfAllowed(User user) {
        Instant now = Instant.now(clock);
        if (user.getEmailVerificationLastSentAt() != null
                && user.getEmailVerificationLastSentAt().plus(VERIFICATION_RESEND_COOLDOWN).isAfter(now)) {
            return;
        }
        String rawToken = setEmailVerificationToken(user, now);
        userRepository.save(user);
        enqueueVerificationEmail(user, rawToken);
    }

    private String setEmailVerificationToken(User user, Instant now) {
        String rawToken = generateVerificationToken();
        user.setEmailVerificationTokenHash(hashToken(rawToken));
        user.setEmailVerificationExpiresAt(now.plus(OTP_EXPIRES_MINUTES, ChronoUnit.MINUTES));
        user.setEmailVerificationLastSentAt(now);
        return rawToken;
    }

    private String setPhoneVerificationToken(User user, Instant now) {
        String rawToken = generateVerificationToken();
        user.setPhoneVerificationTokenHash(hashToken(rawToken));
        user.setPhoneVerificationExpiresAt(now.plus(OTP_EXPIRES_MINUTES, ChronoUnit.MINUTES));
        user.setPhoneVerificationLastSentAt(now);
        return rawToken;
    }

    private String setLoginOtp(User user, Instant now) {
        String rawToken = generateVerificationToken();
        user.setLoginOtpTokenHash(hashToken(rawToken));
        user.setLoginOtpExpiresAt(now.plus(OTP_EXPIRES_MINUTES, ChronoUnit.MINUTES));
        user.setLoginOtpLastSentAt(now);
        return rawToken;
    }

    private void clearEmailVerification(User user) {
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setEmailVerificationLastSentAt(null);
    }

    private void clearPhoneVerification(User user) {
        user.setPhoneVerificationTokenHash(null);
        user.setPhoneVerificationExpiresAt(null);
        user.setPhoneVerificationLastSentAt(null);
    }

    private void clearLoginOtp(User user) {
        user.setLoginOtpTokenHash(null);
        user.setLoginOtpExpiresAt(null);
        user.setLoginOtpLastSentAt(null);
    }

    private void enqueueVerificationEmail(User user, String rawToken) {
        String verifyUrl = publicBaseUrl + "/verify/" + rawToken;
        String body = "Chào mừng bạn đến với HotelBooking.\n\n"
                + "Mã xác thực OTP của bạn là: " + rawToken + "\n"
                + "Mã này có hiệu lực trong " + OTP_EXPIRES_MINUTES + " phút. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.\n\n"
                + "Hoặc bạn có thể bấm trực tiếp vào liên kết sau để xác thực email:\n"
                + verifyUrl + "\n\n"
                + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.";
        emailService.enqueue(user, null, EmailEventType.EMAIL_VERIFICATION,
                user.getEmail(), "Xác thực tài khoản HotelBooking - Mã OTP: " + rawToken, "email-verification", body);
    }

    private void enqueueLoginOtpEmail(User user, String rawToken) {
        String body = "Mã OTP đăng nhập HotelBooking của bạn là: " + rawToken + "\n\n"
                + "Mã này có hiệu lực trong " + OTP_EXPIRES_MINUTES + " phút. Không chia sẻ mã này cho bất kỳ ai.";
        emailService.enqueue(user, null, EmailEventType.LOGIN_OTP,
                user.getEmail(), "Mã OTP đăng nhập HotelBooking: " + rawToken, "login-otp", body);
    }

    private User findUserByIdentifier(String normalizedIdentifier, boolean emailIdentifier) {
        if (emailIdentifier) {
            return userRepository.findByEmailIgnoreCase(normalizedIdentifier)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản phù hợp để gửi OTP."));
        }
        List<User> matches = userRepository.findByPhone(normalizedIdentifier);
        if (matches.isEmpty() && normalizedIdentifier.startsWith("0")) {
            matches = userRepository.findByPhone("+84" + normalizedIdentifier.substring(1));
        }
        if (matches.isEmpty()) {
            throw new BusinessException("Không tìm thấy tài khoản phù hợp để gửi OTP.");
        }
        if (matches.size() > 1) {
            throw new BusinessException("Số điện thoại này đang gắn với nhiều tài khoản. Vui lòng đăng nhập bằng email.");
        }
        return matches.get(0);
    }

    private void ensureCanReceiveLoginOtp(User user) {
        if (user.getStatus() == UserStatus.LOCKED
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now(clock)))) {
            throw new BusinessException("Tài khoản đang bị khóa, vui lòng thử lại sau.");
        }
    }

    private void ensureCooldownElapsed(Instant lastSentAt, Instant now, Duration cooldown, String message) {
        if (lastSentAt != null && lastSentAt.plus(cooldown).isAfter(now)) {
            throw new BusinessException(message);
        }
    }

    private void ensureNotExpired(Instant expiresAt, String message) {
        if (expiresAt == null || expiresAt.isBefore(Instant.now(clock))) {
            throw new BusinessException(message);
        }
    }

    private User reload(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản hiện tại."));
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            throw new BusinessException("Họ tên phải từ 2 đến 100 ký tự.");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("Định dạng email không hợp lệ.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new BusinessException("Số điện thoại Việt Nam không hợp lệ.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessException("Mật khẩu cần ít nhất 8 ký tự, 1 chữ hoa, 1 số và 1 ký tự đặc biệt.");
        }
    }

    private String normalizeIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.contains("@")) {
            String email = normalizeEmail(value);
            validateEmail(email);
            return email;
        }
        String phone = normalizePhone(value);
        validatePhone(phone);
        return phone;
    }

    private String generateVerificationToken() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    private String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Mã OTP không hợp lệ.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException("Không thể kiểm tra mã OTP.");
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

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    public record OtpDelivery(String identifier, String channel, String maskedDestination) {
    }
}
