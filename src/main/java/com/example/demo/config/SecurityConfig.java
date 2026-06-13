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
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

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
            
            String continueUrl = (String) request.getSession().getAttribute("CONTINUE_URL");
            if (continueUrl != null) {
                request.getSession().removeAttribute("CONTINUE_URL");
                new DefaultRedirectStrategy().sendRedirect(request, response, continueUrl);
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
        String paymentFormActionSources = paymentFormActionSources(environment);

        http
                .csrf(csrf -> {
                    // Eager CSRF attributes so Thymeleaf th:action on public pages works (Spring Security 6.4+ deferred tokens).
                    org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler csrfHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
                    csrfHandler.setCsrfRequestAttributeName(null); // Opt-out of deferred CSRF token
                    csrf.csrfTokenRequestHandler(csrfHandler);
                    csrf.ignoringRequestMatchers(paymentEndpoint("/webhook"), pathStartsWith("/api/recommend"), pathStartsWith("/api/chat"), pathStartsWith("/logout"));
                    if (e2eFixtureEnabled) {
                        csrf.ignoringRequestMatchers(pathStartsWith("/__e2e__"));
                    }
                })
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/hotels/**", "/rooms/**", "/login", "/login/password", "/login/otp", "/login/otp/**", "/login-otp", "/forgot-password", "/forgot-password/**", "/login/oauth-mock", "/login/oauth2/**", "/register", "/signup", "/verify/**", "/error",
                            "/ai-recommendation", "/recommend", "/api/recommend", "/api/chat",
                            "/actuator/health", "/actuator/health/**", "/css/**", "/js/**", "/webjars/**", "/favicon.svg").permitAll();
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
                                    "form-action " + paymentFormActionSources + "; " +
                                    "frame-ancestors 'none'; " +
                                    "frame-src 'self' https://www.google.com https://maps.google.com https://translate.google.com; " +
                                    "img-src 'self' data: https: https://maps.googleapis.com https://translate.google.com https://translate.googleapis.com https://www.gstatic.com; " +
                                    "style-src 'self' 'unsafe-inline' https://translate.googleapis.com https://www.gstatic.com https://fonts.googleapis.com; " +
                                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://translate.google.com https://translate.googleapis.com https://translate-pa.googleapis.com https://www.gstatic.com https://cdnjs.cloudflare.com; " +
                                    "font-src 'self' data: https://fonts.gstatic.com; " +
                                    "connect-src 'self' https://translate.google.com https://translate.googleapis.com https://translate-pa.googleapis.com;"));
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

    private static String paymentFormActionSources(Environment environment) {
        Set<String> sources = new LinkedHashSet<>();
        sources.add("'self'");
        addOrigin(sources, environment.getProperty("vnpay.pay-url", ""));
        addOrigin(sources, environment.getProperty("momo.create-url", ""));
        sources.add("https://sandbox.vnpayment.vn");
        sources.add("https://pay.vnpay.vn");
        sources.add("https://test-payment.momo.vn");
        sources.add("https://payment.momo.vn");
        return String.join(" ", sources);
    }

    private static void addOrigin(Set<String> sources, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(stripQuotes(url.trim()));
            if (uri.getScheme() == null || uri.getHost() == null) {
                return;
            }
            int port = uri.getPort();
            String origin = uri.getScheme() + "://" + uri.getHost() + (port > 0 ? ":" + port : "");
            sources.add(origin);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
