package com.example.demo.service;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = null;
        String name = null;
        
        if ("google".equalsIgnoreCase(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else if ("facebook".equalsIgnoreCase(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            if (email == null) {
                // Facebook email can sometimes be null if user registered with phone number.
                // Fall back to id-based mock email.
                String id = (String) attributes.get("id");
                email = id + "@facebook.com";
            }
        }
        
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Không thể lấy địa chỉ email từ tài khoản mạng xã hội của bạn.");
        }
        
        email = email.trim().toLowerCase();
        final String finalEmail = email;
        final String finalName = name != null ? name.trim() : "Social User";
        
        User user = userRepository.findByEmailIgnoreCase(finalEmail).orElseGet(() -> {
            Role userRole = roleRepository.findByCode("USER")
                    .orElseThrow(() -> new IllegalStateException("Thiếu role USER trong hệ thống."));
            User newUser = new User();
            newUser.setEmail(finalEmail);
            newUser.setFullName(finalName);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setEmailVerified(true);
            newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.getRoles().add(userRole);
            return userRepository.save(newUser);
        });
        
        // If the user already existed but was pending verification, activate them
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION || !user.isEmailVerified()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            userRepository.save(user);
        }
        
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .map(GrantedAuthority.class::cast)
                .toList();
        
        java.util.Map<String, Object> customAttributes = new java.util.HashMap<>(attributes);
        customAttributes.put("email", finalEmail);
        customAttributes.put("name", finalName);
                
        return new DefaultOAuth2User(authorities, customAttributes, "email");
    }
}
