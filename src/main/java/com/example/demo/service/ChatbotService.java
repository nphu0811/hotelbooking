package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final HttpClient httpClient;

    public ChatbotService(ObjectMapper objectMapper,
                          @Value("${openai.api.key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    public String getChatResponse(List<Map<String, String>> history) {
        if (apiKey.isBlank() || apiKey.equals("sk-proj-xxxx")) {
            throw new BusinessException("OpenAI API key chưa được cấu hình. Chatbot tạm thời ngưng hoạt động.");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "gpt-4o-mini");

            List<Map<String, String>> messages = new ArrayList<>();
            
            // System message
            Map<String, String> systemMessage = new LinkedHashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Bạn là LUMI, một lễ tân ảo thân thiện và chuyên nghiệp của LUMIÈRE Hotel. " +
                    "Khách sạn có các chi nhánh tại Hà Nội, Đà Nẵng, TP. Hồ Chí Minh với trải nghiệm sang trọng và tinh tế. " +
                    "Nhiệm vụ của bạn là giải đáp thắc mắc, tư vấn dịch vụ, chính sách đặt/hủy phòng và trò chuyện tự nhiên với khách hàng. " +
                    "Hãy trả lời ngắn gọn, thân thiện, dùng biểu tượng cảm xúc phù hợp và dùng tiếng Việt chuẩn.");
            messages.add(systemMessage);

            // Append history
            if (history != null) {
                for (Map<String, String> msg : history) {
                    if (msg.containsKey("role") && msg.containsKey("content")) {
                        Map<String, String> chatMsg = new LinkedHashMap<>();
                        chatMsg.put("role", msg.get("role"));
                        chatMsg.put("content", msg.get("content"));
                        messages.add(chatMsg);
                    }
                }
            }

            body.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("Lỗi kết nối AI: HTTP " + response.statusCode());
            }

            JsonNode responseRoot = objectMapper.readTree(response.body());
            return responseRoot.path("choices").get(0).path("message").path("content").asText();

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Rất tiếc, tôi đang gặp trục trặc hệ thống. Bạn vui lòng thử lại sau nhé!", ex);
        }
    }
}
