package com.example.demo.email;

import com.example.demo.service.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EmailProviderRegistry {
    private final Map<String, EmailProvider> providers;

    public EmailProviderRegistry(List<EmailProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(provider -> key(provider.getProviderName()), Function.identity()));
    }

    public EmailProvider require(String providerName) {
        EmailProvider provider = providers.get(key(providerName));
        if (provider == null) {
            throw new BusinessException("Email provider is not configured: " + providerName);
        }
        return provider;
    }

    private String key(String providerName) {
        return providerName == null ? "" : providerName.toLowerCase(Locale.ROOT);
    }
}
