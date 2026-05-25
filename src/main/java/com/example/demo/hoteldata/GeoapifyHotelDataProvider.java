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
public class GeoapifyHotelDataProvider implements HotelDataProvider {
    private static final String PLACES_ENDPOINT = "https://api.geoapify.com/v2/places";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final boolean enabled;

    public GeoapifyHotelDataProvider(ObjectMapper objectMapper,
                                     @Value("${geoapify.api-key:}") String apiKey,
                                     @Value("${geoapify.enabled:false}") boolean enabled) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.enabled = enabled;
    }

    @Override
    public String getSource() {
        return "geoapify";
    }

    @Override
    public List<HotelDataRecord> fetch(HotelImportRequest request) {
        if (!enabled) {
            throw new BusinessException("Geoapify provider is disabled. Enable it by setting GEOAPIFY_ENABLED=true.");
        }
        if (apiKey.isBlank()) {
            throw new BusinessException("Geoapify API key is required. Set GEOAPIFY_API_KEY before using source=geoapify.");
        }

        BoundingBox bbox = bboxFor(request.city());
        List<HotelDataRecord> records = new ArrayList<>();
        int limit = request.limit() <= 0 ? 100 : request.limit();
        int offset = 0;
        int pageSize = Math.min(limit, 100);

        try {
            while (records.size() < limit) {
                int currentBatchSize = Math.min(pageSize, limit - records.size());
                JsonNode root = queryGeoapify(request.city(), bbox, currentBatchSize, offset);
                int parsed = parseFeatures(root, records, request.city(), bbox.province());
                if (parsed == 0) {
                    break;
                }
                offset += parsed;

                if (records.size() < limit) {
                    try {
                        Thread.sleep(500); // 500ms sleep to respect Geoapify rate limits
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            return records.size() > limit ? records.subList(0, limit) : records;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Geoapify import failed: " + sanitize(ex.getMessage()));
        }
    }

    private JsonNode queryGeoapify(String city, BoundingBox bbox, int limit, int offset) throws Exception {
        String filterParam = "rect:" + bbox.west() + "," + bbox.south() + "," + bbox.east() + "," + bbox.north();
        String urlString = PLACES_ENDPOINT + "?categories=accommodation"
                + "&filter=" + URLEncoder.encode(filterParam, StandardCharsets.UTF_8)
                + "&limit=" + limit
                + "&offset=" + offset
                + "&apiKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HotelBookingPortfolio/1.0 (Geoapify enrichment)")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new BusinessException("Geoapify credentials rejected the import request: " + sanitize(response.body()));
        }
        if (response.statusCode() == 429) {
            throw new BusinessException("Geoapify rate limit reached: " + sanitize(response.body()));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException("Geoapify request failed with status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private int parseFeatures(JsonNode root, List<HotelDataRecord> records, String city, String province) throws Exception {
        JsonNode features = root.get("features");
        if (features == null || !features.isArray() || features.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (JsonNode feature : features) {
            count++;
            JsonNode properties = feature.get("properties");
            if (properties == null) {
                continue;
            }

            String placeId = text(properties, "place_id");
            String name = text(properties, "name");

            // Extract coordinates from geometry.coordinates [lon, lat] or properties
            BigDecimal lat = null;
            BigDecimal lon = null;
            JsonNode geometry = feature.get("geometry");
            if (geometry != null) {
                JsonNode coordinates = geometry.get("coordinates");
                if (coordinates != null && coordinates.isArray() && coordinates.size() >= 2) {
                    lon = new BigDecimal(coordinates.get(0).asText());
                    lat = new BigDecimal(coordinates.get(1).asText());
                }
            }
            if (lat == null || lon == null) {
                lat = properties.has("lat") ? new BigDecimal(properties.get("lat").asText()) : null;
                lon = properties.has("lon") ? new BigDecimal(properties.get("lon").asText()) : null;
            }

            if (placeId == null || name == null || name.isBlank() || lat == null || lon == null) {
                continue;
            }

            String address = first(properties, "formatted", "address_line1");
            String countryCode = first(properties, "country_code", "country");
            if (countryCode != null) {
                countryCode = countryCode.toUpperCase(Locale.ROOT);
            } else {
                countryCode = "VN";
            }

            String phone = null;
            JsonNode contact = properties.get("contact");
            if (contact != null && contact.has("phone")) {
                phone = contact.get("phone").asText();
            }
            if (phone == null && properties.has("phone")) {
                phone = properties.get("phone").asText();
            }

            String website = text(properties, "website");

            // Find sourceUrl
            String sourceUrl = null;
            JsonNode datasource = properties.get("datasource");
            if (datasource != null) {
                if (datasource.has("url")) {
                    sourceUrl = datasource.get("url").asText();
                } else {
                    JsonNode raw = datasource.get("raw");
                    if (raw != null) {
                        String osmType = text(raw, "osm_type");
                        String osmId = text(raw, "osm_id");
                        if (osmType != null && osmId != null) {
                            String type = osmType.equalsIgnoreCase("N") ? "node" :
                                    (osmType.equalsIgnoreCase("W") ? "way" : "relation");
                            sourceUrl = "https://www.openstreetmap.org/" + type + "/" + osmId;
                        }
                    }
                }
            }

            Map<String, String> amenities = new LinkedHashMap<>();
            JsonNode categoriesNode = properties.get("categories");
            if (categoriesNode != null && categoriesNode.isArray()) {
                for (JsonNode cat : categoriesNode) {
                    String val = cat.asText().toLowerCase(Locale.ROOT);
                    if (val.contains("wheelchair")) {
                        amenities.put("wheelchair", "Wheelchair access");
                    }
                    if (val.contains("internet_access") || val.contains("wifi")) {
                        amenities.put("internet_access", "Internet access");
                    }
                    if (val.contains("swimming_pool") || val.contains("pool")) {
                        amenities.put("swimming_pool", "Swimming pool");
                    }
                }
            }

            records.add(new HotelDataRecord(
                    getSource(),
                    placeId,
                    name,
                    address,
                    normalizeCity(city),
                    province,
                    countryCode,
                    lat,
                    lon,
                    null, // Star rating is generally not provided by Geoapify Places
                    phone,
                    website,
                    sourceUrl,
                    null, // Primary image URL
                    null, // Image attribution
                    amenities,
                    feature.toString()
            ));
        }
        return count;
    }

    private BoundingBox bboxFor(String city) {
        String key = normalizeCity(city).toLowerCase(Locale.ROOT);
        return switch (key) {
            case "ho chi minh", "ho chi minh city", "hcmc", "saigon" -> new BoundingBox(10.35, 106.35, 11.15, 107.05, "Ho Chi Minh City");
            case "ha noi", "hanoi" -> new BoundingBox(20.75, 105.55, 21.35, 106.10, "Ha Noi");
            case "da nang", "danang" -> new BoundingBox(15.85, 107.85, 16.25, 108.35, "Da Nang");
            case "da lat", "dalat" -> new BoundingBox(11.85, 108.30, 12.05, 108.55, "Lam Dong");
            case "nha trang" -> new BoundingBox(12.15, 109.05, 12.35, 109.35, "Khanh Hoa");
            case "vung tau" -> new BoundingBox(10.30, 107.00, 10.55, 107.20, "Ba Ria - Vung Tau");
            case "phu quoc" -> new BoundingBox(9.95, 103.80, 10.45, 104.10, "Kien Giang");
            case "hoi an" -> new BoundingBox(15.80, 108.25, 16.00, 108.45, "Quang Nam");
            case "hue" -> new BoundingBox(16.35, 107.45, 16.60, 107.70, "Thua Thien Hue");
            default -> throw new BusinessException("Unsupported import city: " + city);
        };
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null ? null : value.asText();
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

    private String normalizeCity(String city) {
        return city == null || city.isBlank() ? "Ho Chi Minh City" : city.trim();
    }

    private String sanitize(String message) {
        if (message == null) {
            return null;
        }
        String sanitized = message;
        if (!apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "[REDACTED]");
        }
        sanitized = sanitized.replaceAll("(?i)(key|api_key|apikey|token)=[a-zA-Z0-9_-]+", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(key|api_key|apikey|token)\\s+[a-zA-Z0-9_-]+", "$1 [REDACTED]");
        return sanitized;
    }

    private record BoundingBox(double south, double west, double north, double east, String province) {
    }
}
