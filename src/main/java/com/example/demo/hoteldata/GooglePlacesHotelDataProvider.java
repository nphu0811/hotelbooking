package com.example.demo.hoteldata;

import com.example.demo.service.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GooglePlacesHotelDataProvider implements HotelDataProvider {
    private static final String SEARCH_ENDPOINT = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.googleMapsUri",
            "places.nationalPhoneNumber",
            "places.internationalPhoneNumber",
            "places.websiteUri",
            "places.rating",
            "places.userRatingCount",
            "places.photos",
            "places.primaryType",
            "places.types",
            "nextPageToken");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final boolean enabled;

    public GooglePlacesHotelDataProvider(ObjectMapper objectMapper,
                                         @Value("${google.places.api-key:}") String apiKey,
                                         @Value("${google.places.enabled:false}") boolean enabled) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.enabled = enabled;
    }

    @Override
    public String getSource() {
        return "google_places";
    }

    @Override
    public List<HotelDataRecord> fetch(HotelImportRequest request) {
        if (!enabled) {
            throw new BusinessException("Google Places provider is disabled. Enable it by setting GOOGLE_PLACES_ENABLED=true.");
        }
        if (apiKey.isBlank()) {
            throw new BusinessException("Google Places API key is required. Set GOOGLE_PLACES_API_KEY before using source=google_places.");
        }
        try {
            List<HotelDataRecord> records = new ArrayList<>();
            String pageToken = null;
            int pageSize = Math.min(Math.max(request.limit(), 1), 20);
            do {
                JsonNode root = search(request.city(), pageSize, pageToken);
                parsePlaces(root, normalizeCity(request.city()), provinceFor(request.city()), records);
                pageToken = text(root, "nextPageToken");
            } while (pageToken != null && records.size() < request.limit());
            return records.size() > request.limit() ? records.subList(0, request.limit()) : records;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Google Places import failed");
        }
    }

    private JsonNode search(String city, int pageSize, String pageToken) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", "hotels in " + normalizeCity(city) + ", Vietnam");
        body.put("pageSize", pageSize);
        body.put("languageCode", "vi");
        body.put("regionCode", "VN");
        if (pageToken != null && !pageToken.isBlank()) {
            body.put("pageToken", pageToken);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_ENDPOINT))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessException("Google Places credentials rejected the import request: " + googleError(response.body()));
        }
        if (response.statusCode() == 429) {
            throw new BusinessException("Google Places quota/rate limit reached: " + googleError(response.body()));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException("Google Places request failed with status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private void parsePlaces(JsonNode root, String city, String province, List<HotelDataRecord> records) throws Exception {
        JsonNode places = root.get("places");
        if (places == null) {
            return;
        }
        for (JsonNode place : places) {
            String id = text(place, "id");
            String name = nestedText(place, "displayName", "text");
            BigDecimal latitude = decimal(place.get("location"), "latitude");
            BigDecimal longitude = decimal(place.get("location"), "longitude");
            if (id == null || name == null || latitude == null || longitude == null) {
                continue;
            }
            Photo photo = primaryPhoto(place);
            String imageUrl = photo.name() == null ? null : resolvePhotoUri(photo.name());
            records.add(new HotelDataRecord(
                    getSource(),
                    id,
                    name,
                    text(place, "formattedAddress"),
                    city,
                    province,
                    "VN",
                    latitude,
                    longitude,
                    null,
                    first(place, "internationalPhoneNumber", "nationalPhoneNumber"),
                    text(place, "websiteUri"),
                    text(place, "googleMapsUri"),
                    imageUrl,
                    photo.attribution(),
                    amenities(place),
                    sanitizedRawPayload(place, photo.attribution(), imageUrl != null)
            ));
        }
    }

    private Photo primaryPhoto(JsonNode place) {
        JsonNode photos = place.get("photos");
        if (photos == null || photos.isEmpty()) {
            return new Photo(null, null);
        }
        JsonNode firstPhoto = photos.get(0);
        String attribution = null;
        JsonNode attributions = firstPhoto.get("authorAttributions");
        if (attributions != null && !attributions.isEmpty()) {
            attribution = nestedText(attributions.get(0), "displayName");
        }
        return new Photo(text(firstPhoto, "name"), attribution);
    }

    private String resolvePhotoUri(String photoName) throws Exception {
        String encodedName = URLEncoder.encode(photoName, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
        URI uri = URI.create("https://places.googleapis.com/v1/" + encodedName
                + "/media?maxWidthPx=960&skipHttpRedirect=true&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessException("Google Place Photos credentials rejected the import request: " + googleError(response.body()));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        return text(objectMapper.readTree(response.body()), "photoUri");
    }

    private String googleError(String body) {
        try {
            JsonNode error = objectMapper.readTree(body).get("error");
            if (error == null) {
                return "status unavailable";
            }
            String status = text(error, "status");
            String message = text(error, "message");
            if (status == null && message == null) {
                return "status unavailable";
            }
            return (status == null ? "UNKNOWN" : status) + " - " + (message == null ? "" : message);
        } catch (Exception ex) {
            return "status unavailable";
        }
    }

    private Map<String, String> amenities(JsonNode place) {
        Map<String, String> values = new LinkedHashMap<>();
        addType(values, place, "lodging", "Lodging");
        addType(values, place, "hotel", "Hotel");
        return values;
    }

    private void addType(Map<String, String> values, JsonNode place, String type, String label) {
        JsonNode types = place.get("types");
        if (types == null) {
            return;
        }
        for (JsonNode value : types) {
            if (type.equalsIgnoreCase(value.asText())) {
                values.put(type, label);
            }
        }
    }

    private String sanitizedRawPayload(JsonNode place, String attribution, boolean hasPhoto) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", text(place, "id"));
        payload.put("displayName", nestedText(place, "displayName", "text"));
        payload.put("formattedAddress", text(place, "formattedAddress"));
        payload.put("googleMapsUri", text(place, "googleMapsUri"));
        payload.put("rating", text(place, "rating"));
        payload.put("userRatingCount", text(place, "userRatingCount"));
        payload.put("photoResolved", hasPhoto);
        payload.put("photoAttribution", attribution);
        return objectMapper.writeValueAsString(payload);
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null ? null : new BigDecimal(value.asText());
    }

    private String first(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String nestedText(JsonNode node, String... path) {
        JsonNode cursor = node;
        for (String field : path) {
            if (cursor == null) {
                return null;
            }
            cursor = cursor.get(field);
        }
        return cursor == null ? null : cursor.asText();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null ? null : value.asText();
    }

    private String normalizeCity(String city) {
        return city == null || city.isBlank() ? "Ho Chi Minh City" : city.trim();
    }

    private String provinceFor(String city) {
        String key = normalizeCity(city).toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ha noi", "hanoi" -> "Ha Noi";
            case "da nang", "danang" -> "Da Nang";
            case "da lat", "dalat" -> "Lam Dong";
            case "nha trang" -> "Khanh Hoa";
            case "vung tau" -> "Ba Ria - Vung Tau";
            case "phu quoc" -> "Kien Giang";
            case "hoi an" -> "Quang Nam";
            case "hue" -> "Thua Thien Hue";
            default -> normalizeCity(city);
        };
    }

    private record Photo(String name, String attribution) {
    }
}
