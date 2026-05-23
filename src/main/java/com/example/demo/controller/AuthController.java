package com.example.demo.controller;

import com.example.demo.service.AuthService;
import com.example.demo.service.BusinessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Email hoặc mật khẩu không chính xác, hoặc tài khoản chưa ACTIVE");
        }
        if (registered != null) {
            model.addAttribute("message", "Đăng ký thành công. Dùng link verify trong email log hoặc tài khoản demo để đăng nhập.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        try {
            authService.register(fullName, email, phone, password, confirmPassword);
            return "redirect:/login?registered";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            return "auth/register";
        }
    }

    @GetMapping("/verify/{token}")
    public String verify(@PathVariable UUID token, Model model) {
        try {
            authService.verify(token);
            model.addAttribute("message", "Tài khoản đã được xác minh. Bạn có thể đăng nhập.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "auth/login";
    }
}
