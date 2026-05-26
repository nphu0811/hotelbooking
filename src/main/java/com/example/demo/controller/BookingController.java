package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.web.BookingForm;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;
    private final boolean mockPaymentEnabled;
    private final String paymentProvider;

    public BookingController(BookingService bookingService,
                             CurrentUserService currentUserService,
                             @Value("${app.payment.mock.enabled:false}") boolean mockPaymentEnabled,
                             @Value("${app.payment.provider:disabled}") String paymentProvider) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
        this.mockPaymentEnabled = mockPaymentEnabled;
        this.paymentProvider = paymentProvider;
    }

    @GetMapping("/bookings")
    public String getBookings() {
        return "redirect:/";
    }

    @PostMapping("/bookings")
    public String create(@Valid @ModelAttribute BookingForm bookingForm,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireCurrentUser();
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("ADMIN") || role.getCode().equals("SUPER_ADMIN"));
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION && !isAdmin) {
            return "redirect:/verification";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Booking request is invalid.");
            return "error";
        }
        try {
            Booking booking = bookingService.createPendingBooking(
                    user,
                    bookingForm.getRoomId(),
                    bookingForm.getCheckIn(),
                    bookingForm.getCheckOut(),
                    bookingForm.getGuests(),
                    bookingForm.getSpecialRequest());
            return "redirect:/checkout/" + booking.getId();
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/checkout/{id}")
    public String checkout(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        Booking booking = bookingService.requireOwnBooking(user, id);
        model.addAttribute("booking", booking);
        model.addAttribute("mockPaymentEnabled", mockPaymentEnabled);
        model.addAttribute("paymentProviderConfigured", paymentProvider != null
                && !paymentProvider.isBlank()
                && !"disabled".equalsIgnoreCase(paymentProvider));
        return "bookings/checkout";
    }

    @GetMapping("/account/bookings")
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("bookings", bookingService.history(user, PageRequest.of(page, 10)));
        return "bookings/history";
    }

    @GetMapping("/account/bookings/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("booking", bookingService.requireOwnBooking(user, id));
        return "bookings/detail";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        try {
            bookingService.cancel(user, id);
            return "redirect:/account/bookings/" + id + "?cancelled";
        } catch (BusinessException ex) {
            model.addAttribute("booking", bookingService.requireOwnBooking(user, id));
            model.addAttribute("error", ex.getMessage());
            return "bookings/detail";
        }
    }
}
