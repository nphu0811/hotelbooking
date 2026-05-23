package com.example.demo.controller;

import com.example.demo.entity.Room;
import com.example.demo.service.ReviewService;
import com.example.demo.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.UUID;

@Controller
public class RoomController {
    private final RoomService roomService;
    private final ReviewService reviewService;

    public RoomController(RoomService roomService, ReviewService reviewService) {
        this.roomService = roomService;
        this.reviewService = reviewService;
    }

    @GetMapping("/rooms/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Room room = roomService.requireDetail(id);
        model.addAttribute("room", room);
        model.addAttribute("reviews", reviewService.latestFor(room));
        model.addAttribute("checkIn", LocalDate.now().plusDays(1));
        model.addAttribute("checkOut", LocalDate.now().plusDays(3));
        model.addAttribute("guests", Math.min(room.getCapacity(), 2));
        return "rooms/detail";
    }
}
