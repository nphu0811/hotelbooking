package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductionSecurityTests.ProbeController.class)
@Import({SecurityConfig.class, PasswordConfig.class, TimeConfig.class, RateLimitService.class, DebugSurfaceDenyFilterConfig.class,
        ProductionSecurityTests.SecurityTestBeans.class})
@ActiveProfiles("prod")
class ProductionSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void h2ConsoleIsDeniedInProd() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mockPaymentEndpointsAreDeniedInProd() throws Exception {
        mockMvc.perform(post("/payments/mock/callback")
                        .with(csrf())
                        .param("orderId", "MOCK-123")
                        .param("success", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realPaymentWebhookAndReturnArePublicInProd() throws Exception {
        mockMvc.perform(post("/payments/vnpay/webhook")
                        .content("vnp_TxnRef=missing"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/payments/vnpay/return")
                        .param("vnp_TxnRef", "missing"))
                .andExpect(status().isNotFound());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/probe")
        String probe() {
            return "ok";
        }
    }

    @TestConfiguration
    static class SecurityTestBeans {
        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return Mockito.mock(CustomUserDetailsService.class);
        }

        @Bean
        LoginAttemptService loginAttemptService() {
            return Mockito.mock(LoginAttemptService.class);
        }
    }
}
