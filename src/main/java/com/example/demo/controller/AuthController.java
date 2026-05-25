package com.example.demo.controller;

import com.example.demo.service.AuthService;
import com.example.demo.service.BusinessException;
import com.example.demo.web.RegisterForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String captcha,
                        @RequestParam(required = false) String locked,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String resent,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Email or password is incorrect, or the account is not active");
        }
        if (captcha != null) {
            model.addAttribute("captchaRequired", true);
        }
        if (locked != null) {
            model.addAttribute("locked", true);
        }
        if (registered != null) {
            model.addAttribute("message", "If the account can be registered, a verification email will be sent.");
        }
        if (resent != null) {
            model.addAttribute("message", "If the account is waiting for verification, a new email will be sent.");
        }
        return "auth/login";
    }

    @GetMapping({"/register", "/signup"})
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping({"/register", "/signup"})
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please correct the highlighted registration fields.");
            preserveRegisterForm(model, registerForm);
            return "auth/register";
        }
        try {
            authService.register(
                    registerForm.getFullName(),
                    registerForm.getEmail(),
                    registerForm.getPhone(),
                    registerForm.getPassword(),
                    registerForm.getConfirmPassword());
            return "redirect:/login?registered";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            preserveRegisterForm(model, registerForm);
            return "auth/register";
        }
    }

    @PostMapping("/verification/resend")
    public String resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return "redirect:/login?resent";
    }

    @GetMapping("/verify/{token}")
    public String verify(@PathVariable String token, Model model) {
        try {
            authService.verify(token);
            model.addAttribute("message", "Account verified. You can sign in.");
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "auth/login";
    }

    private void preserveRegisterForm(Model model, RegisterForm form) {
        model.addAttribute("fullName", form.getFullName());
        model.addAttribute("email", form.getEmail());
        model.addAttribute("phone", form.getPhone());
        model.addAttribute("registerForm", form);
    }
}
