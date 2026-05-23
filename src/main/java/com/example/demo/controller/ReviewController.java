package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class ReviewController {
    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/bookings/{id}/review")
    public String create(@PathVariable UUID id,
                         @RequestParam int rating,
                         @RequestParam int cleanlinessRating,
                         @RequestParam int serviceRating,
                         @RequestParam int locationRating,
                         @RequestParam int valueRating,
                         @RequestParam String content,
                         Model model) {
        User user = currentUserService.requireCurrentUser();
        try {
            reviewService.create(user, id, rating, cleanlinessRating, serviceRating, locationRating, valueRating, content);
            return "redirect:/account/bookings/" + id + "?reviewed";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }
}
