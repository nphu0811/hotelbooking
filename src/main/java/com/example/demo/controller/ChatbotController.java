package com.example.demo.controller;

import com.example.demo.service.BusinessException;
import com.example.demo.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, String>> history = (List<Map<String, String>>) payload.get("messages");
            String reply = chatbotService.getChatResponse(history);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi xử lý hệ thống."));
        }
    }
}
