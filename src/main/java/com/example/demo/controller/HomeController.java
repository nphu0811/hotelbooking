package com.example.demo.controller;

import com.example.demo.service.BusinessException;
import com.example.demo.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Controller
public class HomeController {
    private final RoomService roomService;
    private final Clock clock;

    public HomeController(RoomService roomService, Clock clock) {
        this.roomService = roomService;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate checkIn = LocalDate.now(clock).plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", 2);
        model.addAttribute("rooms", roomService.search("", checkIn, checkOut, 2, null, null, "rating", 0));
        return "home";
    }

    @GetMapping("/rooms/search")
    public String search(@RequestParam(defaultValue = "") String q,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                         @RequestParam(defaultValue = "1") int guests,
                         @RequestParam(required = false) BigDecimal minPrice,
                         @RequestParam(required = false) BigDecimal maxPrice,
                         @RequestParam(defaultValue = "rating") String sort,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        model.addAttribute("q", q);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        try {
            model.addAttribute("rooms", roomService.search(q, checkIn, checkOut, guests, minPrice, maxPrice, sort, page));
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "rooms/search";
    }
}
