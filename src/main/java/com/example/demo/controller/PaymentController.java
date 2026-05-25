package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.User;
import com.example.demo.payment.PaymentIntent;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class PaymentController {
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/payments/start/{bookingId}")
    public String start(@PathVariable UUID bookingId, Model model, HttpServletRequest request) {
        User user = currentUserService.requireCurrentUser();
        Booking booking = bookingService.requireOwnBooking(user, bookingId);
        try {
            PaymentIntent intent = paymentService.createConfiguredPaymentIntent(booking, clientIp(request));
            return "redirect:" + intent.redirectUrl();
        } catch (BusinessException ex) {
            model.addAttribute("booking", booking);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("paymentProviderConfigured", true);
            model.addAttribute("mockPaymentEnabled", false);
            return "bookings/checkout";
        }
    }

    @PostMapping("/payments/{provider}/webhook")
    public ResponseEntity<String> webhook(@PathVariable String provider, HttpServletRequest request) throws IOException {
        String rawPayload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(name -> name, request::getHeader, (left, right) -> right));
        try {
            Payment payment = paymentService.handleWebhook(provider, headers, rawPayload);
            return ResponseEntity.ok(payment.getStatus().name());
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body("REJECTED");
        }
    }

    @GetMapping("/payments/{provider}/return")
    public String paymentReturn(@PathVariable String provider,
                                @RequestParam Map<String, String> params,
                                Model model) {
        String orderId = params.getOrDefault("vnp_TxnRef", params.get("orderId"));
        if (orderId == null || orderId.isBlank()) {
            model.addAttribute("error", "Payment return is missing the order id.");
            return "bookings/payment-result";
        }
        try {
            Payment payment = paymentService.requirePaymentByOrderId(orderId);
            if (!payment.getProvider().equalsIgnoreCase(provider)) {
                throw new BusinessException("Payment provider mismatch");
            }
            model.addAttribute("payment", payment);
            model.addAttribute("booking", payment.getBooking());
        } catch (BusinessException ex) {
            model.addAttribute("error", "Payment status is not available yet.");
        }
        return "bookings/payment-result";
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
