package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.LoginAttemptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                     PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler(LoginAttemptService loginAttemptService) {
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            loginAttemptService.recordSuccess(authentication, request);
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    AuthenticationFailureHandler authenticationFailureHandler(LoginAttemptService loginAttemptService) {
        RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
        return (request, response, exception) -> {
            var result = loginAttemptService.recordFailure(request.getParameter("username"), exception, request);
            String targetUrl = "/login?error";
            if (result.locked()) {
                targetUrl += "&locked";
            } else if (result.showCaptcha()) {
                targetUrl += "&captcha";
            }
            redirectStrategy.sendRedirect(request, response, targetUrl);
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationSuccessHandler authenticationSuccessHandler,
                                            AuthenticationFailureHandler authenticationFailureHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/rooms/**", "/login", "/register", "/verify/**", "/css/**", "/favicon.svg", "/h2-console/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
