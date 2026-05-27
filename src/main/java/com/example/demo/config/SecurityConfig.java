package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.LoginAttemptService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
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
    AuthenticationSuccessHandler authenticationSuccessHandler(LoginAttemptService loginAttemptService,
                                                              com.example.demo.repository.UserRepository userRepository,
                                                              Environment environment) {
        SavedRequestAwareAuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        delegate.setDefaultTargetUrl("/");
        boolean e2eFixtureEnabled = environment.getProperty("app.e2e-fixture.enabled", Boolean.class, false);
        return (request, response, authentication) -> {
            loginAttemptService.recordSuccess(authentication, request);
            
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
            
            if (isAdmin && !e2eFixtureEnabled) {
                new DefaultRedirectStrategy().sendRedirect(request, response, "/admin");
                return;
            }
            
            String email = authentication.getName();
            com.example.demo.entity.User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user != null && user.getStatus() == com.example.demo.entity.UserStatus.PENDING_VERIFICATION) {
                new DefaultRedirectStrategy().sendRedirect(request, response, "/verification");
                return;
            }
            
            delegate.onAuthenticationSuccess(request, response, authentication);
        };
    }

    @Bean
    AuthenticationFailureHandler authenticationFailureHandler(LoginAttemptService loginAttemptService) {
        RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
        return (request, response, exception) -> {
            var result = loginAttemptService.recordFailure(request.getParameter("username"), exception, request);
            String targetUrl = "/login/password?error";
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
                                            AuthenticationFailureHandler authenticationFailureHandler,
                                            com.example.demo.service.CustomOAuth2UserService customOAuth2UserService,
                                            Environment environment) throws Exception {
        boolean localDebugProfile = environment.acceptsProfiles(Profiles.of("local", "dev", "test"));
        boolean h2ConsoleEnabled = localDebugProfile
                && environment.getProperty("spring.h2.console.enabled", Boolean.class, false);
        boolean e2eFixtureEnabled = localDebugProfile
                && environment.getProperty("app.e2e-fixture.enabled", Boolean.class, false);

        http
                .csrf(csrf -> {
                    csrf.ignoringRequestMatchers(paymentEndpoint("/webhook"));
                    if (e2eFixtureEnabled) {
                        csrf.ignoringRequestMatchers(pathStartsWith("/__e2e__"));
                    }
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/rooms/**", "/login", "/login/password", "/login/otp", "/login/otp/**", "/login-otp", "/login/oauth-mock", "/login/oauth2/**", "/register", "/signup", "/verify/**", "/error",
                            "/actuator/health", "/actuator/health/**", "/css/**", "/js/**", "/favicon.svg").permitAll();
                    auth.requestMatchers(paymentEndpoint("/webhook"), paymentEndpoint("/return")).permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers(pathStartsWith("/h2-console")).permitAll();
                    } else {
                        auth.requestMatchers(pathStartsWith("/h2-console")).denyAll();
                    }
                    if (!localDebugProfile) {
                        auth.requestMatchers(pathStartsWith("/payments/mock")).denyAll();
                    }
                    if (e2eFixtureEnabled) {
                        auth.requestMatchers(pathStartsWith("/__e2e__")).permitAll();
                    } else {
                        auth.requestMatchers(pathStartsWith("/__e2e__")).denyAll();
                    }
                    auth.requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN");
                    auth.anyRequest().authenticated();
                })
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .successHandler(authenticationSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        ))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .headers(headers -> {
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                                    "base-uri 'self'; " +
                                    "form-action 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "img-src 'self' data: https:; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "script-src 'self'"));
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31536000));
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicyHeader(permissions -> permissions
                            .policy("camera=(), microphone=(), geolocation=(self), payment=()"));
                    headers.contentTypeOptions(Customizer.withDefaults());
                    if (h2ConsoleEnabled) {
                        headers.frameOptions(frame -> frame.sameOrigin());
                    } else {
                        headers.frameOptions(frame -> frame.deny());
                    }
                });
        return http.build();
    }

    private static RequestMatcher pathStartsWith(String prefix) {
        return request -> {
            String contextPath = request.getContextPath();
            String path = request.getRequestURI();
            if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            return path.equals(prefix) || path.startsWith(prefix + "/");
        };
    }

    private static RequestMatcher paymentEndpoint(String suffix) {
        return request -> {
            String contextPath = request.getContextPath();
            String path = request.getRequestURI();
            if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
            return path.matches("^/payments/[^/]+" + suffix + "$");
        };
    }
}
