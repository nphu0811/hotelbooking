package com.example.demo.payment;

import com.example.demo.service.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderRegistry {
    private final Map<String, PaymentProvider> providers;

    public PaymentProviderRegistry(List<PaymentProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(provider -> key(provider.getProviderName()), Function.identity()));
    }

    public PaymentProvider require(String providerName) {
        PaymentProvider provider = providers.get(key(providerName));
        if (provider == null) {
            throw new BusinessException("Payment provider is not configured: " + providerName);
        }
        return provider;
    }

    private String key(String providerName) {
        return providerName == null ? "" : providerName.toLowerCase(Locale.ROOT);
    }
}
