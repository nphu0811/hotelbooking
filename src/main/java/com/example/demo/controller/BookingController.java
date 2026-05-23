package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    public BookingController(BookingService bookingService, CurrentUserService currentUserService) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/bookings")
    public String create(@RequestParam UUID roomId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                         @RequestParam int guests,
                         @RequestParam(required = false) String specialRequest,
                         Model model) {
        User user = currentUserService.requireCurrentUser();
        try {
            Booking booking = bookingService.createPendingBooking(user, roomId, checkIn, checkOut, guests, specialRequest);
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
