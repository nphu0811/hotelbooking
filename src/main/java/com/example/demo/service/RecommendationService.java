package com.example.demo.service;

import com.example.demo.entity.Room;
import com.example.demo.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class RecommendationService {
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final HttpClient httpClient;

    public RecommendationService(RoomRepository roomRepository,
                                 ObjectMapper objectMapper,
                                 @Value("${openai.api.key:}") String apiKey) {
        this.roomRepository = roomRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    public List<RecommendationResult> getRecommendations(String userPrompt) {
        if (apiKey.isBlank() || apiKey.equals("sk-proj-xxxx")) {
            throw new BusinessException("OpenAI API key chưa được cấu hình. Vui lòng thiết lập OPENAI_API_KEY trong env.");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new BusinessException("Yêu cầu gợi ý không được để trống.");
        }

        // Fetch active rooms from database
        List<Room> allRooms = roomRepository.findAll().stream()
                .filter(r -> !r.isDeleted() && "AVAILABLE".equalsIgnoreCase(r.getStatus().name()))
                .toList();

        if (allRooms.isEmpty()) {
            return List.of();
        }

        try {
            // Build rooms data payload in a compact format for the AI context
            List<Map<String, Object>> roomsPayload = new ArrayList<>();
            for (Room r : allRooms) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("roomId", r.getId().toString());
                map.put("roomName", r.getName());
                map.put("roomType", r.getRoomType());
                map.put("pricePerNight", r.getPricePerNight());
                map.put("capacity", r.getCapacity());
                map.put("areaSqm", r.getAreaSqm());
                map.put("hotelName", r.getHotel().getName());
                map.put("hotelCity", r.getHotel().getCity());
                map.put("hotelStars", r.getHotel().getStarRating());
                map.put("hotelDescription", r.getHotel().getDescription());
                roomsPayload.add(map);
            }

            // Call OpenAI API
            String requestBody = buildOpenAiRequestBody(roomsPayload, userPrompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException("Yêu cầu OpenAI thất bại với mã lỗi HTTP: " + response.statusCode());
            }

            // Parse response
            JsonNode responseRoot = objectMapper.readTree(response.body());
            String textResponse = responseRoot.path("choices").get(0).path("message").path("content").asText();

            JsonNode recommendationsJson = objectMapper.readTree(textResponse);
            JsonNode recArray = recommendationsJson.path("recommendations");

            List<RecommendationResult> results = new ArrayList<>();
            if (recArray.isArray()) {
                for (JsonNode rec : recArray) {
                    String roomIdStr = rec.path("roomId").asText();
                    String reason = rec.path("reason").asText();
                    if (roomIdStr == null || roomIdStr.isBlank()) {
                        continue;
                    }
                    try {
                        UUID roomId = UUID.fromString(roomIdStr);
                        Optional<Room> matchingRoom = roomRepository.findDetailedById(roomId);
                        matchingRoom.ifPresent(room -> results.add(new RecommendationResult(room, reason)));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return results;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Đã xảy ra lỗi khi kết nối với động cơ gợi ý AI: " + ex.getMessage(), ex);
        }
    }

    private String buildOpenAiRequestBody(List<Map<String, Object>> rooms, String userPrompt) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "gpt-4o-mini");

        Map<String, String> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Bạn là một trợ lý ảo gợi ý phòng khách sạn chuyên nghiệp bằng tiếng Việt. " +
                "Bạn sẽ nhận danh sách phòng dưới định dạng JSON và yêu cầu tự nhiên của người dùng. " +
                "Nhiệm vụ của bạn là phân tích và lựa chọn ra tối đa 3 phòng phù hợp nhất. " +
                "Bạn PHẢI trả về kết quả dưới định dạng JSON có cấu trúc chính xác như sau:\n" +
                "{\n" +
                "  \"recommendations\": [\n" +
                "    {\n" +
                "      \"roomId\": \"UUID của phòng được chọn\",\n" +
                "      \"reason\": \"Giải thích chi tiết bằng tiếng Việt lý do phòng này phù hợp với yêu cầu của người dùng, nhấn mạnh các điểm mạnh liên quan.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Không thêm bất kỳ văn bản nào khác ngoài JSON này.");

        Map<String, String> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        
        Map<String, Object> userContent = new LinkedHashMap<>();
        userContent.put("user_request", userPrompt);
        userContent.put("available_rooms", rooms);
        userMessage.put("content", objectMapper.writeValueAsString(userContent));

        body.put("messages", List.of(systemMessage, userMessage));

        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        return objectMapper.writeValueAsString(body);
    }

    public record RecommendationResult(Room room, String reason) {}
}
