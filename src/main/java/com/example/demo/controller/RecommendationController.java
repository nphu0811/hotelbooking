package com.example.demo.controller;

import com.example.demo.service.BusinessException;
import com.example.demo.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommend")
    public String showRecommendPage(Model model) {
        model.addAttribute("prompt", "");
        return "recommend";
    }

    @PostMapping("/recommend")
    public String getRecommendations(@RequestParam String prompt, Model model) {
        model.addAttribute("prompt", prompt);
        try {
            List<RecommendationService.RecommendationResult> recommendations = recommendationService.getRecommendations(prompt);
            model.addAttribute("recommendations", recommendations);
            if (recommendations.isEmpty()) {
                model.addAttribute("info", "Rất tiếc, AI không tìm thấy phòng nào phù hợp hoàn toàn với mô tả của bạn. Hãy thử thay đổi yêu cầu xem sao nhé!");
            }
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("error", "Đã xảy ra lỗi không mong muốn khi tìm kiếm gợi ý.");
        }
        return "recommend";
    }

    @org.springframework.web.bind.annotation.ResponseBody
    @PostMapping("/api/recommend")
    public org.springframework.http.ResponseEntity<?> getRecommendationsApi(@RequestParam String prompt) {
        try {
            List<RecommendationService.RecommendationResult> recommendations = recommendationService.getRecommendations(prompt);
            List<java.util.Map<String, Object>> response = recommendations.stream().map(rec -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("reason", rec.reason());
                com.example.demo.entity.Room r = rec.room();
                map.put("roomId", r.getId().toString());
                map.put("name", r.getName());
                map.put("roomType", r.getRoomType());
                map.put("pricePerNight", r.getPricePerNight());
                map.put("primaryImageUrl", r.getPrimaryImageUrl() != null ? r.getPrimaryImageUrl() : "/css/room-placeholder.svg");
                map.put("hotelName", r.getHotel().getName());
                map.put("hotelCity", r.getHotel().getCity());
                map.put("areaSqm", r.getAreaSqm());
                map.put("capacity", r.getCapacity());
                return map;
            }).toList();
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (BusinessException ex) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("error", "Đã xảy ra lỗi không mong muốn khi tìm kiếm gợi ý."));
        }
    }
}
