package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final Clock clock;

    public CustomUserDetailsService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không chính xác"));
        if (user.getStatus() == UserStatus.LOCKED
                && user.getLockedUntil() != null
                && user.getLockedUntil().isBefore(Instant.now(clock))) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginCount(0);
            user.setLastFailedLoginAt(null);
            user.setLockedUntil(null);
            user.setLockReason(null);
            userRepository.save(user);
        }
        boolean active = user.getStatus() == UserStatus.ACTIVE;
        boolean notLocked = user.getLockedUntil() == null || user.getLockedUntil().isBefore(Instant.now(clock));
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .map(GrantedAuthority.class::cast)
                .toList();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(!notLocked || user.getStatus() == UserStatus.LOCKED)
                .disabled(!active)
                .build();
    }
}
