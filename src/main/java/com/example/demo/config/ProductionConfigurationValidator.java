package com.example.demo.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Locale;

@Configuration
@Profile("prod")
public class ProductionConfigurationValidator {
    private static final List<String> FORBIDDEN_MARKERS = List.of(
            "mock",
            "changeme",
            "change-me",
            "your-key",
            "your_",
            "default",
            "placeholder",
            "example",
            "${"
    );

    @Bean
    static BeanFactoryPostProcessor validateProductionConfiguration(Environment environment) {
        return beanFactory -> validate(environment);
    }

    static void validate(Environment environment) {
        requireFalse(environment, "app.seed-demo-data");
        requireFalse(environment, "spring.h2.console.enabled");
        requireFalse(environment, "app.payment.mock.enabled");
        requireValue(environment, "spring.datasource.url");
        requireValue(environment, "spring.datasource.username");
        requireValue(environment, "spring.datasource.password");
        String datasourceUrl = value(environment, "spring.datasource.url");
        if (datasourceUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")) {
            throw invalid("spring.datasource.url", "H2 is not allowed in the production profile");
        }

        String emailProvider = requireValue(environment, "app.email.provider").toLowerCase(Locale.ROOT);
        if ("brevo".equals(emailProvider)) {
            requireValue(environment, "brevo.api-key");
            requireValue(environment, "mail.from");
        } else {
            throw invalid("app.email.provider", "production email provider must be brevo");
        }

        String smsProvider = value(environment, "app.sms.provider").toLowerCase(Locale.ROOT);
        if ("brevo".equals(smsProvider)) {
            requireValue(environment, "brevo.api-key");
            requireValue(environment, "brevo.sms.sender");
        } else if (!"console".equals(smsProvider) && !"disabled".equals(smsProvider) && !smsProvider.isBlank()) {
            throw invalid("app.sms.provider", "production SMS provider must be brevo, console, or disabled");
        }

        String paymentProvider = requireValue(environment, "app.payment.provider").toLowerCase(Locale.ROOT);
        if ("vnpay".equals(paymentProvider)) {
            requireValue(environment, "vnpay.tmn-code");
            requireValue(environment, "vnpay.hash-secret");
            requireValue(environment, "vnpay.pay-url");
            requireValue(environment, "vnpay.return-url");
            requireValue(environment, "vnpay.ipn-url");
            requireHttpsUrl(environment, "app.public-base-url");
            requireHttpsUrl(environment, "vnpay.return-url");
            requireHttpsUrl(environment, "vnpay.ipn-url");
            return;
        }
        throw invalid("app.payment.provider", "supported production provider is vnpay");
    }

    private static void requireFalse(Environment environment, String key) {
        if (environment.getProperty(key, Boolean.class, false)) {
            throw invalid(key, "must be false in production");
        }
    }

    private static String requireValue(Environment environment, String key) {
        String currentValue = value(environment, key);
        if (currentValue.isBlank()) {
            throw invalid(key, "must be set in production");
        }
        String normalized = currentValue.toLowerCase(Locale.ROOT);
        for (String marker : FORBIDDEN_MARKERS) {
            if (normalized.contains(marker)) {
                throw invalid(key, "contains forbidden placeholder-like value");
            }
        }
        return currentValue;
    }

    private static void requireHttpsUrl(Environment environment, String key) {
        String currentValue = requireValue(environment, key);
        if (!currentValue.toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw invalid(key, "must be a public HTTPS URL in production");
        }
    }

    private static String value(Environment environment, String key) {
        try {
            String val = environment.getProperty(key, "").trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1).trim();
            }
            return val;
        } catch (RuntimeException ex) {
            throw invalid(key, "must be set in production");
        }
    }

    private static IllegalStateException invalid(String key, String message) {
        return new IllegalStateException("Invalid production configuration: " + key + " " + message);
    }
}
