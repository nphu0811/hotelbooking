package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.ReviewService;
import com.example.demo.web.ReviewForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
                         @Valid @ModelAttribute ReviewForm reviewForm,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireCurrentUser();
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Review request is invalid.");
            return "error";
        }
        try {
            reviewService.create(
                    user,
                    id,
                    reviewForm.getRating(),
                    reviewForm.getCleanlinessRating(),
                    reviewForm.getServiceRating(),
                    reviewForm.getLocationRating(),
                    reviewForm.getValueRating(),
                    reviewForm.getContent());
            return "redirect:/account/bookings/" + id + "?reviewed";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }
}
