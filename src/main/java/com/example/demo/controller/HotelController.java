package com.example.demo.controller;

import com.example.demo.service.BusinessException;
import com.example.demo.service.HotelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
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
                       @RequestParam(defaultValue = "list") String view,
                       @RequestParam(defaultValue = "") String checkIn,
                       @RequestParam(defaultValue = "") String checkOut,
                       @RequestParam(defaultValue = "2") int adults,
                       @RequestParam(defaultValue = "0") int children,
                       @RequestParam(defaultValue = "1") int rooms,
                       @RequestParam(defaultValue = "2") int guests,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String safeView = "list".equalsIgnoreCase(view) ? "list" : "grid";
        String locationLabel = searchLocationLabel(q, city);
        model.addAttribute("q", q);
        model.addAttribute("city", city);
        model.addAttribute("minRating", minRating);
        model.addAttribute("view", safeView);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("adults", adults);
        model.addAttribute("children", children);
        model.addAttribute("rooms", rooms);
        model.addAttribute("guests", guests);
        model.addAttribute("searchLocationLabel", locationLabel);
        model.addAttribute("searchMapEmbedUrl", googleMapEmbedUrl(locationLabel));
        model.addAttribute("searchMapUrl", googleMapSearchUrl(locationLabel));
        model.addAttribute("hotels", hotelService.searchHotels(searchKeyword(q), searchKeyword(city), minRating, page));
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

    private String searchLocationLabel(String keyword, String city) {
        String value = firstPresent(keyword, city);
        if (value.isBlank()) {
            return "Việt Nam";
        }
        String normalized = ascii(value);
        if (normalized.contains("ho chi minh") || normalized.contains("hcm") || normalized.contains("sai gon")
                || normalized.contains("saigon")) {
            return "TP. Hồ Chí Minh";
        }
        if (normalized.contains("da nang") || normalized.contains("danang")) {
            return "Đà Nẵng";
        }
        if (normalized.contains("ha noi") || normalized.contains("hanoi")) {
            return "Hà Nội";
        }
        if (normalized.contains("da lat") || normalized.contains("dalat")) {
            return "Đà Lạt";
        }
        if (normalized.contains("vung tau")) {
            return "Vũng Tàu";
        }
        if (normalized.contains("nha trang")) {
            return "Nha Trang";
        }
        return value.trim();
    }

    private String searchKeyword(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = ascii(value)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.contains("ho chi minh") || normalized.contains("hcm") || normalized.contains("hcmc")
                || normalized.contains("sai gon") || normalized.contains("saigon")) {
            return "Hồ Chí Minh";
        }
        return value.trim();
    }

    private String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "";
    }

    private String googleMapEmbedUrl(String locationLabel) {
        return "https://www.google.com/maps?q=" + encodedMapQuery(locationLabel) + "&output=embed";
    }

    private String googleMapSearchUrl(String locationLabel) {
        return "https://www.google.com/maps/search/?api=1&query=" + encodedMapQuery(locationLabel);
    }

    private String encodedMapQuery(String locationLabel) {
        String query = locationLabel == null || locationLabel.isBlank() ? "Việt Nam" : locationLabel.trim();
        if (!ascii(query).contains("viet nam")) {
            query = query + ", Việt Nam";
        }
        return URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private String ascii(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
