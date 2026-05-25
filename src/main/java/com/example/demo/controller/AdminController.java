package com.example.demo.controller;

import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.service.AdminService;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class AdminController {
    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    public AdminController(AdminService adminService, CurrentUserService currentUserService) {
        this.adminService = adminService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("rooms", adminService.rooms(PageRequest.of(0, 5)));
        model.addAttribute("bookings", adminService.bookings(PageRequest.of(0, 5)));
        model.addAttribute("users", adminService.users(PageRequest.of(0, 5)));
        model.addAttribute("stats", adminService.dashboardStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/rooms")
    public String rooms(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("rooms", adminService.rooms(PageRequest.of(page, 20)));
        model.addAttribute("statuses", RoomStatus.values());
        return "admin/rooms";
    }

    @PostMapping("/admin/rooms/{id}")
    public String updateRoom(@PathVariable UUID id,
                             @RequestParam(required = false) BigDecimal price,
                             @RequestParam(required = false) RoomStatus status,
                             Model model) {
        User actor = currentUserService.requireCurrentUser();
        try {
            adminService.updateRoom(actor, id, price, status);
            return "redirect:/admin/rooms?updated";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("rooms", adminService.rooms(PageRequest.of(0, 20)));
            model.addAttribute("statuses", RoomStatus.values());
            return "admin/rooms";
        }
    }

    @GetMapping("/admin/bookings")
    public String bookings(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("bookings", adminService.bookings(PageRequest.of(page, 20)));
        return "admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/check-in")
    public String checkIn(@PathVariable UUID id) {
        adminService.checkIn(currentUserService.requireCurrentUser(), id);
        return "redirect:/admin/bookings?checkedIn";
    }

    @PostMapping("/admin/bookings/{id}/check-out")
    public String checkOut(@PathVariable UUID id) {
        adminService.checkOut(currentUserService.requireCurrentUser(), id);
        return "redirect:/admin/bookings?checkedOut";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", adminService.users(PageRequest.of(page, 20)));
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/lock")
    public String lockUser(@PathVariable UUID id, @RequestParam(defaultValue = "Admin lock") String reason) {
        adminService.lockUser(currentUserService.requireCurrentUser(), id, reason);
        return "redirect:/admin/users?locked";
    }

    @PostMapping("/admin/users/{id}/unlock")
    public String unlockUser(@PathVariable UUID id) {
        adminService.unlockUser(currentUserService.requireCurrentUser(), id);
        return "redirect:/admin/users?unlocked";
    }
}
