package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.service.AuthService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.CurrentUserService;
import com.example.demo.web.RegisterForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final CustomUserDetailsService customUserDetailsService;
    private final CurrentUserService currentUserService;
    private final com.example.demo.repository.UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService,
                          CustomUserDetailsService customUserDetailsService,
                          CurrentUserService currentUserService,
                          com.example.demo.repository.UserRepository userRepository) {
        this.authService = authService;
        this.customUserDetailsService = customUserDetailsService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String captcha,
                        @RequestParam(required = false) String locked,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String resent,
                        Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
            if (isAdmin) {
                return "redirect:/admin";
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user != null && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                return "redirect:/verification";
            }
            return "redirect:/";
        }
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
            if (isAdmin) {
                return "redirect:/admin";
            }
            
            String email = authentication.getName();
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user != null && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                return "redirect:/verification";
            }
            return "redirect:/";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping({"/register", "/signup"})
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           Model model,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please correct the highlighted registration fields.");
            preserveRegisterForm(model, registerForm);
            return "auth/register";
        }
        try {
            User user = authService.register(
                    registerForm.getFullName(),
                    registerForm.getEmail(),
                    registerForm.getPhone(),
                    registerForm.getPassword(),
                    registerForm.getConfirmPassword());

            // Programmatically authenticate user upon successful registration
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, userDetails.getPassword(), userDetails.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return "redirect:/verification";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            preserveRegisterForm(model, registerForm);
            return "auth/register";
        }
    }

    @GetMapping("/verification")
    public String verificationForm(Model model) {
        try {
            User user = currentUserService.requireCurrentUser();
            if (user.getStatus() == UserStatus.ACTIVE) {
                return "redirect:/";
            }
            model.addAttribute("email", user.getEmail());
            return "auth/verification";
        } catch (Exception ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/verification")
    public String verifyOtp(@RequestParam String otp, Model model) {
        try {
            User user = currentUserService.requireCurrentUser();
            authService.verifyOtp(user.getEmail(), otp);

            // Refresh security authentication state to ACTIVE
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, userDetails.getPassword(), userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            return "redirect:/?verified";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            try {
                model.addAttribute("email", currentUserService.requireCurrentUser().getEmail());
            } catch (Exception ignored) {}
            return "auth/verification";
        } catch (Exception ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/verification/resend")
    public String resendOtp(Model model) {
        try {
            User user = currentUserService.requireCurrentUser();
            authService.resendVerification(user.getEmail());
            model.addAttribute("message", "Mã OTP mới đã được gửi vào email của bạn.");
            model.addAttribute("email", user.getEmail());
            return "auth/verification";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            try {
                model.addAttribute("email", currentUserService.requireCurrentUser().getEmail());
            } catch (Exception ignored) {}
            return "auth/verification";
        } catch (Exception ex) {
            return "redirect:/login";
        }
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
