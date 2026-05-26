package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public ProfileController(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = currentUserService.requireCurrentUser();
        
        // Disable profile for admin: redirect to admin dashboard
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("ADMIN") || role.getCode().equals("SUPER_ADMIN"));
        if (isAdmin) {
            return "redirect:/admin";
        }
        
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phone,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Model model) {
        User user = currentUserService.requireCurrentUser();
        
        // Disable profile for admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("ADMIN") || role.getCode().equals("SUPER_ADMIN"));
        if (isAdmin) {
            return "redirect:/admin";
        }

        try {
            userService.updateProfile(user, fullName, phone, currentPassword, newPassword, confirmPassword);
            model.addAttribute("message", "Cập nhật thông tin cá nhân thành công");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        
        model.addAttribute("user", user);
        return "profile";
    }
}
