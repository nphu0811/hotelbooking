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
import java.util.HashMap;
import java.util.Map;
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
        var hotels = adminService.hotels(PageRequest.of(0, 5));
        model.addAttribute("hotels", hotels);
        model.addAttribute("bookings", adminService.bookings(PageRequest.of(0, 5)));
        model.addAttribute("users", adminService.users(PageRequest.of(0, 5)));
        model.addAttribute("stats", adminService.dashboardStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/rooms")
    public String roomsLegacy() {
        return "redirect:/admin/hotels";
    }

    @GetMapping("/admin/hotels")
    public String hotels(@RequestParam(defaultValue = "0") int page, Model model) {
        var hotelPage = adminService.hotels(PageRequest.of(page, 20));
        Map<UUID, Long> roomCounts = new HashMap<>();
        for (var hotel : hotelPage.getContent()) {
            roomCounts.put(hotel.getId(), adminService.roomCountForHotel(hotel.getId()));
        }
        model.addAttribute("hotels", hotelPage);
        model.addAttribute("roomCounts", roomCounts);
        return "admin/hotels";
    }

    @PostMapping("/admin/hotels")
    public String createHotel(@RequestParam String name,
                              @RequestParam String city,
                              @RequestParam String province,
                              @RequestParam String address,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) Integer starRating,
                              Model model) {
        User actor = currentUserService.requireCurrentUser();
        try {
            adminService.createHotel(actor, name, city, province, address, description, starRating);
            return "redirect:/admin/hotels?created";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("hotels", adminService.hotels(PageRequest.of(0, 20)));
            model.addAttribute("roomCounts", Map.of());
            return "admin/hotels";
        }
    }

    @PostMapping("/admin/hotels/{hotelId}")
    public String updateHotel(@PathVariable UUID hotelId,
                              @RequestParam(required = false) String name,
                              @RequestParam(required = false) String city,
                              @RequestParam(required = false) String province,
                              @RequestParam(required = false) String address,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) Integer starRating) {
        User actor = currentUserService.requireCurrentUser();
        adminService.updateHotel(actor, hotelId, name, city, province, address, description, starRating);
        return "redirect:/admin/hotels?updated";
    }

    @PostMapping("/admin/hotels/{hotelId}/delete")
    public String deleteHotel(@PathVariable UUID hotelId) {
        User actor = currentUserService.requireCurrentUser();
        adminService.deleteHotel(actor, hotelId);
        return "redirect:/admin/hotels?deleted";
    }

    @GetMapping("/admin/hotels/{hotelId}/rooms")
    public String hotelRooms(@PathVariable UUID hotelId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        var hotel = adminService.requireHotel(hotelId);
        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", adminService.roomsForHotel(hotelId, PageRequest.of(page, 20)));
        model.addAttribute("statuses", RoomStatus.values());
        return "admin/rooms";
    }

    @PostMapping("/admin/hotels/{hotelId}/rooms")
    public String createRoom(@PathVariable UUID hotelId,
                             @RequestParam String name,
                             @RequestParam String roomType,
                             @RequestParam int capacity,
                             @RequestParam BigDecimal pricePerNight,
                             @RequestParam(required = false) String description,
                             Model model) {
        User actor = currentUserService.requireCurrentUser();
        try {
            adminService.createRoom(actor, hotelId, name, roomType, capacity, pricePerNight, description);
            return "redirect:/admin/hotels/" + hotelId + "/rooms?created";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("hotel", adminService.requireHotel(hotelId));
            model.addAttribute("rooms", adminService.roomsForHotel(hotelId, PageRequest.of(0, 20)));
            model.addAttribute("statuses", RoomStatus.values());
            return "admin/rooms";
        }
    }

    @PostMapping("/admin/hotels/{hotelId}/rooms/{roomId}")
    public String updateRoomForHotel(@PathVariable UUID hotelId,
                                   @PathVariable UUID roomId,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) String roomType,
                                   @RequestParam(required = false) Integer capacity,
                                   @RequestParam(required = false) BigDecimal price,
                                   @RequestParam(required = false) RoomStatus status,
                                   @RequestParam(required = false) String description) {
        User actor = currentUserService.requireCurrentUser();
        adminService.updateRoomForHotel(actor, hotelId, roomId, name, roomType, capacity, price, status, description);
        return "redirect:/admin/hotels/" + hotelId + "/rooms?updated";
    }

    @PostMapping("/admin/hotels/{hotelId}/rooms/{roomId}/delete")
    public String deleteRoomForHotel(@PathVariable UUID hotelId, @PathVariable UUID roomId) {
        User actor = currentUserService.requireCurrentUser();
        adminService.deleteRoomForHotel(actor, hotelId, roomId);
        return "redirect:/admin/hotels/" + hotelId + "/rooms?deleted";
    }

    @PostMapping("/admin/rooms/{id}")
    public String updateRoomLegacy(@PathVariable UUID id,
                                   @RequestParam(required = false) BigDecimal price,
                                   @RequestParam(required = false) RoomStatus status) {
        User actor = currentUserService.requireCurrentUser();
        adminService.updateRoom(actor, id, price, status);
        return "redirect:/admin/hotels";
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
