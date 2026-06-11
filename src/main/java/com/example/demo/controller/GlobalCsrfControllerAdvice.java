package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalCsrfControllerAdvice {

    /**
     * Eagerly resolves the CSRF token before view rendering.
     * This prevents IllegalStateException: Cannot create a session after the response has been committed
     * when Thymeleaf automatically injects CSRF tokens into large pages.
     */
    @ModelAttribute
    public void forceCsrfSessionCreation(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // Forces SaveOnAccessCsrfToken to generate and save, creating session if needed safely.
        }
    }
}
