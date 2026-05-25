package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.PaymentService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@Profile({"local", "dev", "test"})
public class LocalMockPaymentController {
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public LocalMockPaymentController(PaymentService paymentService,
                                      BookingService bookingService,
                                      CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/payments/mock/start/{bookingId}")
    public String start(@PathVariable UUID bookingId) {
        User user = currentUserService.requireCurrentUser();
        Booking booking = bookingService.requireOwnBooking(user, bookingId);
        Payment payment = paymentService.startMockPayment(booking);
        return "redirect:/payments/mock/" + payment.getOrderId();
    }

    @GetMapping("/payments/mock/{orderId}")
    public String mockPay(@PathVariable String orderId, Model model) {
        model.addAttribute("orderId", orderId);
        return "bookings/mock-payment";
    }

    @PostMapping("/payments/mock/callback")
    public String callback(@RequestParam String orderId,
                           @RequestParam(defaultValue = "true") boolean success,
                           Model model) {
        try {
            Payment payment = paymentService.completeLocalMockPayment(orderId, success);
            model.addAttribute("payment", payment);
            model.addAttribute("booking", payment.getBooking());
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "bookings/payment-result";
    }
}
