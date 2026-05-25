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
public class OverpassHotelDataProvider implements HotelDataProvider {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;

    public OverpassHotelDataProvider(ObjectMapper objectMapper,
                                     @Value("${hoteldata.overpass.endpoint:https://overpass-api.de/api/interpreter}") String endpoint) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
    }

    @Override
    public String getSource() {
        return "overpass";
    }

    @Override
    public List<HotelDataRecord> fetch(HotelImportRequest request) {
        BoundingBox bbox = bboxFor(request.city());
        String query = """
                [out:json][timeout:25];
                (
                  node["tourism"~"hotel|guest_house|hostel|apartment|chalet|resort"](%s);
                  way["tourism"~"hotel|guest_house|hostel|apartment|chalet|resort"](%s);
                  relation["tourism"~"hotel|guest_house|hostel|apartment|chalet|resort"](%s);
                );
                out center tags %d;
                """.formatted(bbox.toOverpass(), bbox.toOverpass(), bbox.toOverpass(), Math.max(request.limit(), 1));
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", "HotelBookingPortfolio/1.0 (legal OSM Overpass import)")
                    .POST(HttpRequest.BodyPublishers.ofString("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429 || response.statusCode() == 504) {
                throw new BusinessException("Overpass rate/resource limit response: " + response.statusCode());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Overpass request failed with status " + response.statusCode());
            }
            return parse(response.body(), request.city(), bbox.province());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Overpass import failed");
        }
    }

    private List<HotelDataRecord> parse(String body, String city, String province) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        List<HotelDataRecord> records = new ArrayList<>();
        for (JsonNode element : root.get("elements")) {
            JsonNode tags = element.get("tags");
            if (tags == null || tags.get("name") == null) {
                continue;
            }
            BigDecimal lat = coordinate(element, "lat");
            BigDecimal lng = coordinate(element, "lon");
            JsonNode center = element.get("center");
            if ((lat == null || lng == null) && center != null) {
                lat = coordinate(center, "lat");
                lng = coordinate(center, "lon");
            }
            if (lat == null || lng == null) {
                continue;
            }
            String type = text(element, "type");
            String id = text(element, "id");
            String externalId = type + "/" + id;
            Map<String, String> amenities = amenities(tags);
            records.add(new HotelDataRecord(
                    getSource(),
                    externalId,
                    text(tags, "name"),
                    address(tags),
                    normalizeCity(city),
                    province,
                    "VN",
                    lat,
                    lng,
                    star(tags),
                    first(tags, "phone", "contact:phone"),
                    first(tags, "website", "contact:website"),
                    "https://www.openstreetmap.org/" + type + "/" + id,
                    null,
                    "OpenStreetMap contributors",
                    amenities,
                    element.toString()
            ));
        }
        return records;
    }

    private BigDecimal coordinate(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null ? null : new BigDecimal(value.asText());
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

    private String address(JsonNode tags) {
        String full = first(tags, "addr:full", "addr:street");
        String house = first(tags, "addr:housenumber");
        if (full == null) {
            return "";
        }
        return house == null ? full : house + " " + full;
    }

    private Integer star(JsonNode tags) {
        String value = first(tags, "stars", "hotel:stars");
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> amenities(JsonNode tags) {
        Map<String, String> values = new LinkedHashMap<>();
        addIfPresent(values, tags, "internet_access", "Internet access");
        addIfPresent(values, tags, "wheelchair", "Wheelchair access");
        addIfPresent(values, tags, "swimming_pool", "Swimming pool");
        return values;
    }

    private void addIfPresent(Map<String, String> values, JsonNode tags, String key, String label) {
        String value = first(tags, key);
        if (value != null && !value.equalsIgnoreCase("no")) {
            values.put(key, label);
        }
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

    private String normalizeCity(String city) {
        return city == null ? "" : city.trim();
    }

    private record BoundingBox(double south, double west, double north, double east, String province) {
        String toOverpass() {
            return south + "," + west + "," + north + "," + east;
        }
    }
}
