package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final AuthService authService;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public ProfileController(CurrentUserService currentUserService,
                             UserService userService,
                             AuthService authService,
                             CustomUserDetailsService customUserDetailsService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.authService = authService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }

        try {
            User saved = userService.updateProfile(user, fullName, email, phone, currentPassword, newPassword, confirmPassword);
            refreshAuthentication(saved, request, response);
            model.addAttribute("message", "Cập nhật thông tin cá nhân thành công.");
            model.addAttribute("user", saved);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", user);
        }
        return "profile";
    }

    @PostMapping("/profile/verify-email/request")
    public String requestEmailVerification(Model model) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }
        try {
            User saved = authService.requestEmailVerification(user);
            model.addAttribute("message", saved.isEmailVerified()
                    ? "Email đã được xác thực."
                    : "Mã OTP xác thực email đã được gửi.");
            model.addAttribute("showEmailOtp", !saved.isEmailVerified());
            model.addAttribute("user", saved);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", user);
        }
        return "profile";
    }

    @PostMapping("/profile/verify-email")
    public String verifyEmail(@RequestParam String otp,
                              Model model,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }
        try {
            User saved = authService.verifyEmailOtp(user, otp);
            refreshAuthentication(saved, request, response);
            model.addAttribute("message", "Email đã được xác thực.");
            model.addAttribute("user", saved);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("showEmailOtp", true);
            model.addAttribute("user", user);
        }
        return "profile";
    }

    @PostMapping("/profile/verify-phone/request")
    public String requestPhoneVerification(Model model) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }
        try {
            User saved = authService.requestPhoneVerification(user);
            model.addAttribute("message", saved.isPhoneVerified()
                    ? "Số điện thoại đã được xác thực."
                    : "Mã OTP xác thực số điện thoại đã được gửi.");
            model.addAttribute("showPhoneOtp", !saved.isPhoneVerified());
            model.addAttribute("user", saved);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", user);
        }
        return "profile";
    }

    @PostMapping("/profile/verify-phone")
    public String verifyPhone(@RequestParam String otp, Model model) {
        User user = currentUserService.requireCurrentUser();
        if (isAdmin(user)) {
            return "redirect:/admin";
        }
        try {
            User saved = authService.verifyPhoneOtp(user, otp);
            model.addAttribute("message", "Số điện thoại đã được xác thực.");
            model.addAttribute("user", saved);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("showPhoneOtp", true);
            model.addAttribute("user", user);
        }
        return "profile";
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("ADMIN") || role.getCode().equals("SUPER_ADMIN"));
    }

    private void refreshAuthentication(User user,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);
    }
}
