package com.example.demo.controller;

import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.service.BookingService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import com.example.demo.web.BookingForm;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUserService currentUserService;
    private final boolean mockPaymentEnabled;
    private final String paymentProvider;
    private final String vnpayTmnCode;
    private final String vnpayHashSecret;
    private final String vnpayPayUrl;
    private final String vnpayReturnUrl;
    private final String vnpayIpnUrl;
    private final String momoPartnerCode;
    private final String momoAccessKey;
    private final String momoSecretKey;
    private final String momoCreateUrl;
    private final String momoReturnUrl;
    private final String momoIpnUrl;
    private final Clock clock;

    public BookingController(BookingService bookingService,
                             CurrentUserService currentUserService,
                             @Value("${app.payment.mock.enabled:false}") boolean mockPaymentEnabled,
                             @Value("${app.payment.provider:disabled}") String paymentProvider,
                             @Value("${vnpay.tmn-code:}") String vnpayTmnCode,
                             @Value("${vnpay.hash-secret:}") String vnpayHashSecret,
                             @Value("${vnpay.pay-url:}") String vnpayPayUrl,
                             @Value("${vnpay.return-url:}") String vnpayReturnUrl,
                             @Value("${vnpay.ipn-url:}") String vnpayIpnUrl,
                             @Value("${momo.partner-code:}") String momoPartnerCode,
                             @Value("${momo.access-key:}") String momoAccessKey,
                             @Value("${momo.secret-key:}") String momoSecretKey,
                             @Value("${momo.create-url:}") String momoCreateUrl,
                             @Value("${momo.return-url:}") String momoReturnUrl,
                             @Value("${momo.ipn-url:}") String momoIpnUrl,
                             Clock clock) {
        this.bookingService = bookingService;
        this.currentUserService = currentUserService;
        this.mockPaymentEnabled = mockPaymentEnabled;
        this.paymentProvider = paymentProvider;
        this.vnpayTmnCode = vnpayTmnCode;
        this.vnpayHashSecret = vnpayHashSecret;
        this.vnpayPayUrl = vnpayPayUrl;
        this.vnpayReturnUrl = vnpayReturnUrl;
        this.vnpayIpnUrl = vnpayIpnUrl;
        this.momoPartnerCode = momoPartnerCode;
        this.momoAccessKey = momoAccessKey;
        this.momoSecretKey = momoSecretKey;
        this.momoCreateUrl = momoCreateUrl;
        this.momoReturnUrl = momoReturnUrl;
        this.momoIpnUrl = momoIpnUrl;
        this.clock = clock;
    }

    @GetMapping("/bookings")
    public String getBookings() {
        return "redirect:/";
    }

    @PostMapping("/bookings")
    public String create(@Valid @ModelAttribute BookingForm bookingForm,
                         BindingResult bindingResult,
                         Model model) {
        User user = currentUserService.requireCurrentUser();
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equals("ADMIN") || role.getCode().equals("SUPER_ADMIN"));
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION && !isAdmin) {
            return "redirect:/verification";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Booking request is invalid.");
            return "error";
        }
        try {
            Booking booking = bookingService.createPendingBooking(
                    user,
                    bookingForm.getRoomId(),
                    bookingForm.getCheckIn(),
                    bookingForm.getCheckOut(),
                    bookingForm.getGuests(),
                    bookingForm.getSpecialRequest());
            return "redirect:/checkout/" + booking.getId();
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/checkout/{id}")
    public String checkout(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        Booking booking = bookingService.requireOwnBooking(user, id);
        addCheckoutModel(model, booking);
        return "bookings/checkout";
    }

    @GetMapping("/account/bookings")
    public String history(@RequestParam(defaultValue = "0") int page, Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("bookings", bookingService.history(user, PageRequest.of(page, 10)));
        return "bookings/history";
    }

    @GetMapping("/account/bookings/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("booking", bookingService.requireOwnBooking(user, id));
        return "bookings/detail";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable UUID id, Model model) {
        User user = currentUserService.requireCurrentUser();
        try {
            bookingService.cancel(user, id);
            return "redirect:/account/bookings/" + id + "?cancelled";
        } catch (BusinessException ex) {
            model.addAttribute("booking", bookingService.requireOwnBooking(user, id));
            model.addAttribute("error", ex.getMessage());
            return "bookings/detail";
        }
    }

    private void addCheckoutModel(Model model, Booking booking) {
        String selectedProvider = model.asMap().get("selectedPaymentProvider") instanceof String selected
                ? selected
                : paymentProvider;
        List<PaymentMethodOption> methods = paymentMethods(selectedProvider);
        if (methods.stream().noneMatch(method -> method.selected() && method.configured())) {
            String firstConfigured = methods.stream()
                    .filter(PaymentMethodOption::configured)
                    .map(PaymentMethodOption::code)
                    .findFirst()
                    .orElse(selectedProvider);
            methods = paymentMethods(firstConfigured);
        }
        boolean hasConfiguredMethod = methods.stream().anyMatch(PaymentMethodOption::configured);
        boolean bookingPayable = booking.getStatus().name().equals("PENDING_PAYMENT")
                && (booking.getExpiresAt() == null || booking.getExpiresAt().isAfter(Instant.now(clock)));
        model.addAttribute("booking", booking);
        model.addAttribute("paymentMethods", methods);
        model.addAttribute("hasConfiguredPaymentMethod", hasConfiguredMethod);
        model.addAttribute("bookingPayable", bookingPayable);
    }

    private List<PaymentMethodOption> paymentMethods(String selectedProvider) {
        List<PaymentMethodOption> baseMethods = new java.util.ArrayList<>();
        if (mockPaymentEnabled || "mock".equalsIgnoreCase(paymentProvider)) {
            baseMethods.add(new PaymentMethodOption(
                    "mock",
                    "Thanh toán thử nghiệm",
                    "Luồng thanh toán nội bộ cho môi trường local/test.",
                    "MOCK",
                    mockPaymentEnabled,
                    isSelected("mock", selectedProvider),
                    mockPaymentEnabled ? "Local" : "Chưa bật"
            ));
        }
        baseMethods.add(new PaymentMethodOption(
                "momo",
                "MoMo",
                "Ví điện tử MoMo.",
                "MoMo",
                configured(momoPartnerCode, momoAccessKey, momoSecretKey, momoCreateUrl, momoReturnUrl, momoIpnUrl),
                isSelected("momo", selectedProvider),
                configured(momoPartnerCode, momoAccessKey, momoSecretKey, momoCreateUrl, momoReturnUrl, momoIpnUrl)
                        ? "Khả dụng" : "Chưa cấu hình"
        ));
        baseMethods.add(new PaymentMethodOption(
                "vnpay",
                "VNPAY",
                "Thẻ ATM, tài khoản ngân hàng và QR qua VNPAY.",
                "VNPAY",
                configured(vnpayTmnCode, vnpayHashSecret, vnpayPayUrl, vnpayReturnUrl, vnpayIpnUrl),
                isSelected("vnpay", selectedProvider),
                configured(vnpayTmnCode, vnpayHashSecret, vnpayPayUrl, vnpayReturnUrl, vnpayIpnUrl)
                        ? "Khả dụng" : "Chưa cấu hình"
        ));
        return baseMethods;
    }

    private boolean configured(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isSelected(String provider, String selectedProvider) {
        return provider.equalsIgnoreCase(selectedProvider == null ? "" : selectedProvider);
    }

    public record PaymentMethodOption(String code,
                                      String name,
                                      String description,
                                      String shortName,
                                      boolean configured,
                                      boolean selected,
                                      String state) {
    }
}
