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
}
