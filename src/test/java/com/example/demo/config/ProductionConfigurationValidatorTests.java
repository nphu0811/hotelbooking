package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTests {
    @Test
    void rejectsH2DatasourceInProd() {
        MockEnvironment environment = completeVnpayEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:prod");

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("H2 is not allowed");
    }

    @Test
    void rejectsPlaceholderSecretsInProd() {
        MockEnvironment environment = completeVnpayEnvironment()
                .withProperty("vnpay.hash-secret", "your-key-here");

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vnpay.hash-secret");
    }

    @Test
    void acceptsCompleteVnpayConfiguration() {
        MockEnvironment environment = completeVnpayEnvironment();

        assertThatCode(() -> ProductionConfigurationValidator.validate(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonHttpsPublicUrlsInProd() {
        MockEnvironment environment = completeVnpayEnvironment()
                .withProperty("app.public-base-url", "http://hotelbooking.local");

        assertThatThrownBy(() -> ProductionConfigurationValidator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.public-base-url");
    }

    private MockEnvironment completeVnpayEnvironment() {
        return new MockEnvironment()
                .withProperty("app.seed-demo-data", "false")
                .withProperty("spring.h2.console.enabled", "false")
                .withProperty("app.payment.mock.enabled", "false")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db.internal:5432/hotelbooking")
                .withProperty("spring.datasource.username", "hotelbooking_app")
                .withProperty("spring.datasource.password", "prod-db-password-rotated")
                .withProperty("mail.host", "smtp.mailhost.internal")
                .withProperty("mail.port", "587")
                .withProperty("mail.username", "mailer-user")
                .withProperty("mail.password", "prod-mail-password-rotated")
                .withProperty("mail.from", "noreply@hotelbooking.local")
                .withProperty("app.email.provider", "smtp")
                .withProperty("app.public-base-url", "https://hotelbooking.local")
                .withProperty("app.payment.provider", "vnpay")
                .withProperty("vnpay.tmn-code", "VNPAYTMNCODE")
                .withProperty("vnpay.hash-secret", "rotated-vnpay-hmac-secret")
                .withProperty("vnpay.pay-url", "https://pay.vnpay.vn/paymentv2/vpcpay.html")
                .withProperty("vnpay.return-url", "https://hotelbooking.local/payments/vnpay/return")
                .withProperty("vnpay.ipn-url", "https://hotelbooking.local/payments/vnpay/ipn");
    }
}
