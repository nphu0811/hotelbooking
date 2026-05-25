package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DebugSurfaceDenyFilterConfig {
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> productionDebugSurfaceDenyFilter(Environment environment) {
        boolean localDebugProfile = environment.acceptsProfiles(Profiles.of("local", "dev", "test"));
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setEnabled(!localDebugProfile);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String path = request.getRequestURI();
                String contextPath = request.getContextPath();
                if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
                    path = path.substring(contextPath.length());
                }
                if (path.equals("/h2-console")
                        || path.startsWith("/h2-console/")
                        || path.equals("/payments/mock")
                        || path.startsWith("/payments/mock/")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        return registration;
    }
}
