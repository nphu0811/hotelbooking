package com.example.demo.controller;

import com.example.demo.service.BusinessException;
import com.example.demo.service.HotelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/hotels")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String city,
                       @RequestParam(required = false) Integer minRating,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("q", q);
        model.addAttribute("city", city);
        model.addAttribute("minRating", minRating);
        model.addAttribute("hotels", hotelService.searchHotels(q, city, minRating, page));
        return "hotels/list";
    }

    @GetMapping("/hotels/{hotelId}")
    public String detail(@PathVariable UUID hotelId, Model model) {
        try {
            var detail = hotelService.requireHotelDetail(hotelId);
            var hotel = detail.hotel();
            model.addAttribute("hotel", hotel);
            model.addAttribute("rooms", detail.rooms());
            model.addAttribute("mapEmbedUrl", hotelService.googleMapsEmbedUrl(hotelService.requireHotel(hotelId)));
            model.addAttribute("hasGoogleMapsApiKey", hotelService.hasGoogleMapsApiKey());
            return "hotels/detail";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "error";
        }
    }

    @GetMapping("/hotels/{hotelId}/rooms/{roomId}")
    public String roomRedirect(@PathVariable UUID hotelId,
                               @PathVariable UUID roomId,
                               RedirectAttributes redirectAttributes) {
        try {
            hotelService.requireRoomForHotel(hotelId, roomId);
            return "redirect:/rooms/" + roomId;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/hotels/" + hotelId;
        }
    }
}
