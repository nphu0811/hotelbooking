package com.example.demo.config;

import com.example.demo.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;

    public RequestRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && !allowed(request)) {
            response.sendError(429, "Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowed(HttpServletRequest request) {
        String path = normalizedPath(request);
        String ip = clientIp(request);
        if ("/login".equals(path)) {
            String email = normalize(request.getParameter("username"));
            return rateLimitService.tryAcquire("ip:login:" + ip, 20, Duration.ofMinutes(15))
                    && rateLimitService.tryAcquire("email:login:" + email, 10, Duration.ofMinutes(15));
        }
        if ("/register".equals(path)) {
            String email = normalize(request.getParameter("email"));
            return rateLimitService.tryAcquire("ip:register:" + ip, 8, Duration.ofHours(1))
                    && rateLimitService.tryAcquire("email:register:" + email, 3, Duration.ofHours(1));
        }
        if ("/verification/resend".equals(path)) {
            String email = normalize(request.getParameter("email"));
            return rateLimitService.tryAcquire("ip:verification-resend:" + ip, 8, Duration.ofHours(1))
                    && rateLimitService.tryAcquire("email:verification-resend:" + email, 3, Duration.ofHours(1));
        }
        return true;
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
