package com.example.demo.hoteldata;

import com.example.demo.service.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class HotelDataProviderRegistry {
    private final Map<String, HotelDataProvider> providers;

    public HotelDataProviderRegistry(List<HotelDataProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(provider -> key(provider.getSource()), Function.identity()));
    }

    public HotelDataProvider require(String source) {
        HotelDataProvider provider = providers.get(key(source));
        if (provider == null) {
            throw new BusinessException("Hotel data provider is not configured: " + source);
        }
        return provider;
    }

    private String key(String source) {
        return source == null ? "" : source.toLowerCase(Locale.ROOT);
    }
}
