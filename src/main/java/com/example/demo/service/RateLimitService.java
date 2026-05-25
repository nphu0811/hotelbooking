package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, int limit, Duration window) {
        if (key == null || key.isBlank()) {
            key = "unknown";
        }
        String normalizedKey = key.trim().toLowerCase();
        Instant now = Instant.now(clock);
        Window result = windows.compute(normalizedKey, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt().isAfter(now)) {
                return new Window(1, now.plus(window));
            }
            if (existing.count() >= limit) {
                return existing;
            }
            return new Window(existing.count() + 1, existing.expiresAt());
        });
        return result.count() <= limit;
    }

    private record Window(int count, Instant expiresAt) {
    }
}
