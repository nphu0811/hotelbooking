package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.User;
import com.example.demo.payment.PaymentIntent;
import com.example.demo.payment.PaymentWebhookResult;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(path = "/payments/{provider}/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> webhook(@PathVariable String provider, HttpServletRequest request) throws IOException {
        String rawPayload = webhookPayload(request);
        Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(name -> name, request::getHeader, (left, right) -> right));
        try {
            PaymentWebhookResult result = paymentService.handleWebhookDetailed(provider, headers, rawPayload);
            if ("vnpay".equalsIgnoreCase(provider)) {
                if (result.alreadyProcessed()) {
                    return vnpayIpn("02", "Order already confirmed");
                }
                return vnpayIpn("00", "Confirm Success");
            }
            return ResponseEntity.ok(result.payment().getStatus().name());
        } catch (BusinessException ex) {
            if ("vnpay".equalsIgnoreCase(provider)) {
                return vnpayIpn(vnpayRspCode(ex), vnpayMessage(ex));
            }
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

    private String webhookPayload(HttpServletRequest request) throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                return request.getQueryString();
            }
            return request.getParameterMap().entrySet().stream()
                    .flatMap(entry -> java.util.Arrays.stream(entry.getValue())
                            .map(value -> encode(entry.getKey()) + "=" + encode(value)))
                    .collect(Collectors.joining("&"));
        }
        return StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ResponseEntity<Map<String, String>> vnpayIpn(String code, String message) {
        return ResponseEntity.ok(Map.of("RspCode", code, "Message", message));
    }

    private String vnpayRspCode(BusinessException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (message.contains("signature") || message.contains("checksum")) {
            return "97";
        }
        if (message.contains("amount")) {
            return "04";
        }
        if (message.contains("not found") || message.contains("unknown payment")) {
            return "01";
        }
        return "99";
    }

    private String vnpayMessage(BusinessException ex) {
        return switch (vnpayRspCode(ex)) {
            case "97" -> "Invalid Checksum";
            case "04" -> "Invalid Amount";
            case "01" -> "Order not Found";
            default -> "Unknown error";
        };
    }
}
