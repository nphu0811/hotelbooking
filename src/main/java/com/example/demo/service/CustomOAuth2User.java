package com.example.demo.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {
    private final String fullName;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes, String nameAttributeKey, String fullName) {
        super(authorities, attributes, nameAttributeKey);
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
