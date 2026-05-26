package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean phoneVerified = false;

    @Column(length = 128)
    private String emailVerificationTokenHash;

    private Instant emailVerificationExpiresAt;

    private Instant emailVerificationLastSentAt;

    @Column(length = 128)
    private String phoneVerificationTokenHash;

    private Instant phoneVerificationExpiresAt;

    private Instant phoneVerificationLastSentAt;

    @Column(length = 128)
    private String loginOtpTokenHash;

    private Instant loginOtpExpiresAt;

    private Instant loginOtpLastSentAt;

    @Column(nullable = false)
    private int failedLoginCount = 0;

    private Instant lastFailedLoginAt;

    private Instant lockedUntil;

    private String lockReason;

    @Column(nullable = false)
    private boolean emailInvalid = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getEmailVerificationTokenHash() {
        return emailVerificationTokenHash;
    }

    public void setEmailVerificationTokenHash(String emailVerificationTokenHash) {
        this.emailVerificationTokenHash = emailVerificationTokenHash;
    }

    public Instant getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
    }

    public void setEmailVerificationExpiresAt(Instant emailVerificationExpiresAt) {
        this.emailVerificationExpiresAt = emailVerificationExpiresAt;
    }

    public Instant getEmailVerificationLastSentAt() {
        return emailVerificationLastSentAt;
    }

    public void setEmailVerificationLastSentAt(Instant emailVerificationLastSentAt) {
        this.emailVerificationLastSentAt = emailVerificationLastSentAt;
    }

    public String getPhoneVerificationTokenHash() {
        return phoneVerificationTokenHash;
    }

    public void setPhoneVerificationTokenHash(String phoneVerificationTokenHash) {
        this.phoneVerificationTokenHash = phoneVerificationTokenHash;
    }

    public Instant getPhoneVerificationExpiresAt() {
        return phoneVerificationExpiresAt;
    }

    public void setPhoneVerificationExpiresAt(Instant phoneVerificationExpiresAt) {
        this.phoneVerificationExpiresAt = phoneVerificationExpiresAt;
    }

    public Instant getPhoneVerificationLastSentAt() {
        return phoneVerificationLastSentAt;
    }

    public void setPhoneVerificationLastSentAt(Instant phoneVerificationLastSentAt) {
        this.phoneVerificationLastSentAt = phoneVerificationLastSentAt;
    }

    public String getLoginOtpTokenHash() {
        return loginOtpTokenHash;
    }

    public void setLoginOtpTokenHash(String loginOtpTokenHash) {
        this.loginOtpTokenHash = loginOtpTokenHash;
    }

    public Instant getLoginOtpExpiresAt() {
        return loginOtpExpiresAt;
    }

    public void setLoginOtpExpiresAt(Instant loginOtpExpiresAt) {
        this.loginOtpExpiresAt = loginOtpExpiresAt;
    }

    public Instant getLoginOtpLastSentAt() {
        return loginOtpLastSentAt;
    }

    public void setLoginOtpLastSentAt(Instant loginOtpLastSentAt) {
        this.loginOtpLastSentAt = loginOtpLastSentAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    public Instant getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public void setLastFailedLoginAt(Instant lastFailedLoginAt) {
        this.lastFailedLoginAt = lastFailedLoginAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public boolean isEmailInvalid() {
        return emailInvalid;
    }

    public void setEmailInvalid(boolean emailInvalid) {
        this.emailInvalid = emailInvalid;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
