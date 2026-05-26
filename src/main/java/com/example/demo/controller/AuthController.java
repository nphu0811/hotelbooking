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
import org.springframework.security.authentication.AuthenticationManager;
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
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService,
                          CustomUserDetailsService customUserDetailsService,
                          CurrentUserService currentUserService,
                          com.example.demo.repository.UserRepository userRepository,
                          AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.customUserDetailsService = customUserDetailsService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String captcha,
                        @RequestParam(required = false) String locked,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String resent,
                        Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isRealAuthentication(authentication)) {
            if (isAdmin(authentication)) {
                return "redirect:/admin";
            }
            User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
            if (user != null && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                return "redirect:/verification";
            }
            return "redirect:/";
        }
        if (error != null) {
            model.addAttribute("passwordError", "Email hoặc mật khẩu không chính xác, hoặc tài khoản chưa hoạt động.");
        }
        if (captcha != null) {
            model.addAttribute("captchaRequired", true);
        }
        if (locked != null) {
            model.addAttribute("locked", true);
        }
        if (registered != null) {
            model.addAttribute("message", "Nếu tài khoản có thể đăng ký, mã xác thực đã được gửi đến email.");
        }
        if (resent != null) {
            model.addAttribute("message", "Nếu tài khoản đang chờ xác thực, mã mới đã được gửi.");
        }
        return "auth/login";
    }

    @PostMapping("/login/otp/request")
    public String requestLoginOtp(@RequestParam String identifier, Model model) {
        try {
            AuthService.OtpDelivery delivery = authService.requestLoginOtp(identifier);
            model.addAttribute("identifier", delivery.identifier());
            model.addAttribute("channel", delivery.channel());
            model.addAttribute("maskedDestination", delivery.maskedDestination());
            model.addAttribute("message", "Mã OTP đã được gửi.");
            return "auth/login-otp";
        } catch (BusinessException ex) {
            model.addAttribute("otpError", ex.getMessage());
            model.addAttribute("identifier", identifier);
            return "auth/login";
        }
    }

    @PostMapping("/login/otp/verify")
    public String verifyLoginOtp(@RequestParam String identifier,
                                 @RequestParam String otp,
                                 Model model,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        try {
            User user = authService.verifyLoginOtp(identifier, otp);
            authenticateUser(user, request, response);
            if (user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()) || "SUPER_ADMIN".equals(role.getCode()))) {
                return "redirect:/admin";
            }
            return "redirect:/";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("identifier", identifier);
            model.addAttribute("channel", authService.isEmailIdentifier(identifier) ? "email" : "phone");
            model.addAttribute("maskedDestination", identifier);
            return "auth/login-otp";
        }
    }

    @GetMapping({"/register", "/signup"})
    public String registerForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isRealAuthentication(authentication)) {
            if (isAdmin(authentication)) {
                return "redirect:/admin";
            }
            User user = userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
            if (user != null && user.getStatus() == UserStatus.PENDING_VERIFICATION) {
                return "redirect:/verification";
            }
            return "redirect:/";
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping({"/register", "/signup"})
    public String register(@Valid @ModelAttribute RegisterForm registerForm,
                           BindingResult bindingResult,
                           Model model,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng kiểm tra lại các trường đăng ký.");
            return "auth/register";
        }
        try {
            User user = authService.register(
                    registerForm.getFullName(),
                    registerForm.getEmail(),
                    registerForm.getPhone(),
                    registerForm.getPassword(),
                    registerForm.getConfirmPassword());
            authenticateByPassword(user.getEmail(), registerForm.getPassword(), request, response);
            return "redirect:/verification";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verification")
    public String verificationForm(Model model) {
        try {
            User user = currentUserService.requireCurrentUser();
            if (user.getStatus() == UserStatus.ACTIVE && user.isEmailVerified()) {
                return "redirect:/";
            }
            model.addAttribute("email", user.getEmail());
            return "auth/verification";
        } catch (Exception ex) {
            return "redirect:/login";
        }
    }

    @PostMapping("/verification")
    public String verifyOtp(@RequestParam String otp,
                            Model model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        try {
            User user = currentUserService.requireCurrentUser();
            authService.verifyOtp(user.getEmail(), otp);
            User refreshed = userRepository.findByEmailIgnoreCase(user.getEmail()).orElse(user);
            authenticateUser(refreshed, request, response);
            return "redirect:/?verified";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            try {
                model.addAttribute("email", currentUserService.requireCurrentUser().getEmail());
            } catch (Exception ignored) {
            }
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
            } catch (Exception ignored) {
            }
            return "auth/verification";
        } catch (Exception ex) {
            return "redirect:/login";
        }
    }

    @GetMapping("/verify/{token}")
    public String verify(@PathVariable String token, Model model) {
        try {
            authService.verify(token);
            model.addAttribute("message", "Email đã được xác thực. Bạn có thể đăng nhập.");
        } catch (BusinessException ex) {
            model.addAttribute("otpError", ex.getMessage());
        }
        return "auth/login";
    }

    private void authenticateByPassword(String email,
                                        String rawPassword,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        saveAuthentication(authentication, request, response);
    }

    private void authenticateUser(User user, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        saveAuthentication(authentication, request, response);
    }

    private void saveAuthentication(Authentication authentication,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);
    }

    private boolean isRealAuthentication(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
