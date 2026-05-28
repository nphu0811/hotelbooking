package com.example.demo.controller;

import com.example.demo.entity.Room;
import com.example.demo.service.HotelService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Controller
public class RoomController {
    private final RoomService roomService;
    private final HotelService hotelService;
    private final ReviewService reviewService;
    private final Clock clock;

    public RoomController(RoomService roomService, HotelService hotelService,
                          ReviewService reviewService, Clock clock) {
        this.roomService = roomService;
        this.hotelService = hotelService;
        this.reviewService = reviewService;
        this.clock = clock;
    }

    @GetMapping("/rooms/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Room room = roomService.requireDetail(id);
        model.addAttribute("room", room);
        if (room.getHotel() != null && !room.getHotel().isDeleted()) {
            model.addAttribute("hotelCard", hotelService.toCard(room.getHotel()));
        }
        model.addAttribute("reviews", reviewService.latestFor(room));
        model.addAttribute("checkIn", LocalDate.now(clock).plusDays(1));
        model.addAttribute("checkOut", LocalDate.now(clock).plusDays(3));
        model.addAttribute("guests", Math.min(room.getCapacity(), 2));
        return "rooms/detail";
    }
}
