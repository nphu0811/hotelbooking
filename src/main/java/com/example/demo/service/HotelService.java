package com.example.demo.service;

import com.example.demo.entity.Hotel;
import com.example.demo.entity.Room;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HotelService {
    private static final String DEFAULT_THUMBNAIL =
            "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80";
    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1_000L);
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private static final Pattern PRICE_WITH_UNIT = Pattern.compile(
            "(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|m|nghin|ngan|k)\\b");
    private static final Pattern PRICE_NEAR_BUDGET_WORD = Pattern.compile(
            "(?:duoi|toi da|max|ngan sach|gia|budget|<=|<)[^0-9]{0,24}(\\d[\\d\\.,]{4,})");
    private static final Pattern RATING_PATTERN = Pattern.compile("([1-5])\\s*(sao|star)\\b");

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final String googleMapsApiKey;

    public HotelService(HotelRepository hotelRepository,
                        RoomRepository roomRepository,
                        @Value("${google.maps.api-key:}") String googleMapsApiKey) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.googleMapsApiKey = googleMapsApiKey == null ? "" : googleMapsApiKey.trim();
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> searchHotels(String keyword, String city, Integer minRating, int page) {
        return searchHotels(keyword, city, minRating, null, PageRequest.of(Math.max(page, 0), 20, defaultSort()));
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> searchHotels(String keyword, String city, Integer minRating, Pageable pageable) {
        return searchHotels(keyword, city, minRating, null, pageable);
    }

    @Transactional(readOnly = true)
    public HotelSearchResult searchHotels(String keyword, String city, Integer minRating,
                                          BigDecimal maxPrice, String smartFilter, int page) {
        HotelSearchFilters filters = resolveSearchFilters(keyword, city, minRating, maxPrice, smartFilter);
        Page<HotelCard> hotels = searchHotels(
                filters.keyword(),
                filters.city(),
                filters.minRating(),
                filters.maxPrice(),
                PageRequest.of(Math.max(page, 0), 20, defaultSort()));
        return new HotelSearchResult(hotels, filters.keyword(), filters.city(), filters.minRating(),
                filters.maxPrice(), filters.smartSummary());
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> searchHotels(String keyword, String city, Integer minRating,
                                        BigDecimal maxPrice, Pageable pageable) {
        String safeKeyword = clean(keyword);
        String safeCity = clean(city);
        BigDecimal safeMaxPrice = normalizePrice(maxPrice);
        Page<Hotel> hotels = hotelRepository.searchActive(
                safeKeyword,
                ascii(safeKeyword),
                safeCity,
                ascii(safeCity),
                minRating,
                safeMaxPrice,
                pageable);
        List<HotelCard> cards = hotels.getContent().stream()
                .map(this::toCard)
                .toList();
        return new PageImpl<>(cards, pageable, hotels.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> featuredHotels(int size) {
        return searchHotels("", "", null, null, PageRequest.of(0, Math.max(size, 1), defaultSort()));
    }

    @Transactional(readOnly = true)
    public Page<Hotel> allHotelsAdmin(Pageable pageable) {
        return hotelRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Hotel> activeHotelsAdmin(Pageable pageable) {
        return hotelRepository.findAllByDeletedFalse(pageable);
    }

    @Transactional
    public Hotel createHotel(String name, String city, String province, String address, String description, Integer starRating) {
        validateHotelFields(name, city, province, address);
        Hotel hotel = new Hotel();
        hotel.setName(name.trim());
        hotel.setSlug(slug(name));
        hotel.setCity(city.trim());
        hotel.setProvince(province.trim());
        hotel.setAddress(address.trim());
        hotel.setAddressLine(address.trim());
        hotel.setDescription(description == null ? "" : description.trim());
        hotel.setStarRating(starRating);
        hotel.setSource("MANUAL");
        return hotelRepository.save(hotel);
    }

    @Transactional
    public Hotel updateHotel(UUID hotelId, String name, String city, String province, String address,
                             String description, Integer starRating) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khách sạn"));
        if (name != null && !name.isBlank()) {
            hotel.setName(name.trim());
            hotel.setSlug(slug(name));
        }
        if (city != null && !city.isBlank()) {
            hotel.setCity(city.trim());
        }
        if (province != null && !province.isBlank()) {
            hotel.setProvince(province.trim());
        }
        if (address != null && !address.isBlank()) {
            hotel.setAddress(address.trim());
            hotel.setAddressLine(address.trim());
        }
        if (description != null) {
            hotel.setDescription(description.trim());
        }
        if (starRating != null) {
            hotel.setStarRating(starRating);
        }
        return hotelRepository.save(hotel);
    }

    @Transactional
    public void deleteHotel(UUID hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khách sạn"));
        hotel.setDeleted(true);
        hotelRepository.save(hotel);
    }

    public boolean hasGoogleMapsApiKey() {
        return !googleMapsApiKey.isBlank();
    }

    public String googleMapsEmbedUrl(Hotel hotel) {
        if (googleMapsApiKey.isBlank()) {
            return null;
        }
        if (hotel.getLatitude() != null && hotel.getLongitude() != null) {
            String query = hotel.getLatitude() + "," + hotel.getLongitude();
            return "https://www.google.com/maps/embed/v1/place?key="
                    + URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8)
                    + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }
        String query = URLEncoder.encode(hotel.getName() + " " + hotel.getAddress(), StandardCharsets.UTF_8);
        return "https://www.google.com/maps/embed/v1/place?key="
                + URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8)
                + "&q=" + query;
    }

    private void validateHotelFields(String name, String city, String province, String address) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Tên khách sạn không được để trống");
        }
        if (city == null || city.isBlank()) {
            throw new BusinessException("Thành phố không được để trống");
        }
        if (province == null || province.isBlank()) {
            throw new BusinessException("Tỉnh/thành không được để trống");
        }
        if (address == null || address.isBlank()) {
            throw new BusinessException("Địa chỉ không được để trống");
        }
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "hotel" : normalized;
    }

    @Transactional(readOnly = true)
    public Hotel requireHotel(UUID hotelId) {
        return hotelRepository.findByIdAndDeletedFalse(hotelId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy khách sạn"));
    }

    @Transactional(readOnly = true)
    public HotelDetail requireHotelDetail(UUID hotelId) {
        Hotel hotel = requireHotel(hotelId);
        return new HotelDetail(toCard(hotel), roomRepository.findActiveByHotelId(hotelId));
    }

    @Transactional(readOnly = true)
    public List<Room> activeRooms(UUID hotelId) {
        requireHotel(hotelId);
        return roomRepository.findActiveByHotelId(hotelId);
    }

    @Transactional(readOnly = true)
    public List<Room> availableRooms(UUID hotelId) {
        requireHotel(hotelId);
        return roomRepository.findAvailableByHotelId(hotelId);
    }

    @Transactional(readOnly = true)
    public Room requireRoom(UUID roomId) {
        Room room = roomRepository.findDetailedById(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng"));
        if (room.isDeleted() || room.getHotel() == null || room.getHotel().isDeleted()) {
            throw new BusinessException("Không tìm thấy phòng");
        }
        return room;
    }

    @Transactional(readOnly = true)
    public Room requireRoomForHotel(UUID hotelId, UUID roomId) {
        requireHotel(hotelId);
        return roomRepository.findActiveByHotelIdAndId(hotelId, roomId)
                .orElseThrow(() -> new BusinessException("Phòng không thuộc khách sạn này"));
    }

    public HotelCard toCard(Hotel hotel) {
        long roomCount = roomRepository.countByHotelAndDeletedFalse(hotel);
        BigDecimal minPrice = roomRepository.minAvailablePriceByHotel(hotel);
        String thumbnail = resolveThumbnail(hotel);
        return new HotelCard(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getAddress(),
                hotel.getDescription(),
                hotel.getStarRating(),
                hotel.isDeleted() ? "DISABLED" : "ACTIVE",
                thumbnail,
                minPrice,
                roomCount,
                hotel.getLatitude(),
                hotel.getLongitude(),
                googleMapsUrl(hotel),
                googleStaticMapUrl(hotel)
        );
    }

    private String resolveThumbnail(Hotel hotel) {
        if (hotel.getThumbnailUrl() != null && !hotel.getThumbnailUrl().isBlank()) {
            return hotel.getThumbnailUrl();
        }
        return roomRepository.findFirstByHotelAndDeletedFalseOrderByCreatedAtAsc(hotel)
                .map(Room::getPrimaryImageUrl)
                .filter(value -> value != null && !value.isBlank())
                .orElseGet(() -> fallbackImage(hotel.getCity()));
    }

    private Sort defaultSort() {
        return Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("name"));
    }

    private HotelSearchFilters resolveSearchFilters(String keyword, String city, Integer minRating,
                                                    BigDecimal maxPrice, String smartFilter) {
        String safeKeyword = clean(keyword);
        String safeCity = clean(city);
        BigDecimal safeMaxPrice = normalizePrice(maxPrice);
        String safeSmartFilter = clean(smartFilter);
        if (safeSmartFilter.isBlank()) {
            return new HotelSearchFilters(safeKeyword, safeCity, minRating, safeMaxPrice, "");
        }

        String normalizedPrompt = ascii(safeSmartFilter);
        String smartCity = detectSmartCity(normalizedPrompt);
        String smartKeyword = detectSmartKeyword(normalizedPrompt);
        Integer smartRating = detectSmartRating(normalizedPrompt);
        BigDecimal smartMaxPrice = detectSmartBudget(normalizedPrompt);

        String effectiveKeyword = safeKeyword.isBlank() ? smartKeyword : safeKeyword;
        String effectiveCity = safeCity.isBlank() ? smartCity : safeCity;
        Integer effectiveRating = minRating != null ? minRating : smartRating;
        BigDecimal effectiveMaxPrice = safeMaxPrice != null ? safeMaxPrice : smartMaxPrice;

        return new HotelSearchFilters(
                effectiveKeyword,
                effectiveCity,
                effectiveRating,
                effectiveMaxPrice,
                smartSummary(smartCity, smartKeyword, smartRating, smartMaxPrice));
    }

    private String detectSmartCity(String normalizedPrompt) {
        if (containsAny(normalizedPrompt, "ho chi minh", "hcm", "hcmc", "sai gon", "saigon")) {
            return "Hồ Chí Minh";
        }
        if (containsAny(normalizedPrompt, "da nang", "danang")) {
            return "Đà Nẵng";
        }
        if (containsAny(normalizedPrompt, "ha noi", "hanoi")) {
            return "Hà Nội";
        }
        if (containsAny(normalizedPrompt, "da lat", "dalat")) {
            return "Đà Lạt";
        }
        if (containsAny(normalizedPrompt, "vung tau")) {
            return "Vũng Tàu";
        }
        if (containsAny(normalizedPrompt, "nha trang")) {
            return "Nha Trang";
        }
        if (containsAny(normalizedPrompt, "phu quoc")) {
            return "Phú Quốc";
        }
        if (containsAny(normalizedPrompt, "hoi an")) {
            return "Hội An";
        }
        return "";
    }

    private String detectSmartKeyword(String normalizedPrompt) {
        if (containsAny(normalizedPrompt, "gan bien", "sat bien", "view bien", "bai bien")) {
            return "biển";
        }
        if (containsAny(normalizedPrompt, "trung tam", "gan pho co", "gan cho dem")) {
            return "trung tâm";
        }
        if (containsAny(normalizedPrompt, "san bay", "gan may bay")) {
            return "sân bay";
        }
        if (containsAny(normalizedPrompt, "resort", "khu nghi duong")) {
            return "resort";
        }
        if (containsAny(normalizedPrompt, "can ho", "apartment")) {
            return "căn hộ";
        }
        if (containsAny(normalizedPrompt, "biet thu", "villa")) {
            return "biệt thự";
        }
        if (containsAny(normalizedPrompt, "yen tinh", "lang man", "honeymoon", "tuan trang mat")) {
            return "yên tĩnh";
        }
        return "";
    }

    private Integer detectSmartRating(String normalizedPrompt) {
        Matcher matcher = RATING_PATTERN.matcher(normalizedPrompt);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if (containsAny(normalizedPrompt, "danh gia tot", "rat tot", "cao cap", "sang trong")) {
            return 4;
        }
        return null;
    }

    private BigDecimal detectSmartBudget(String normalizedPrompt) {
        BigDecimal result = null;
        Matcher unitMatcher = PRICE_WITH_UNIT.matcher(normalizedPrompt);
        while (unitMatcher.find()) {
            BigDecimal value = toBudgetValue(unitMatcher.group(1), unitMatcher.group(2));
            if (value != null && (result == null || value.compareTo(result) > 0)) {
                result = value;
            }
        }
        if (result != null) {
            return result;
        }

        Matcher budgetMatcher = PRICE_NEAR_BUDGET_WORD.matcher(normalizedPrompt);
        if (budgetMatcher.find()) {
            String digits = budgetMatcher.group(1).replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                BigDecimal value = normalizePrice(new BigDecimal(digits));
                if (value != null && value.compareTo(BigDecimal.valueOf(50_000L)) >= 0) {
                    return value;
                }
            }
        }
        return null;
    }

    private BigDecimal toBudgetValue(String number, String unit) {
        if (number == null || unit == null) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(number.replace(',', '.'));
            String normalizedUnit = unit.toLowerCase(Locale.ROOT);
            if (normalizedUnit.equals("trieu") || normalizedUnit.equals("tr") || normalizedUnit.equals("m")) {
                value = value.multiply(ONE_MILLION);
            } else if (normalizedUnit.equals("nghin") || normalizedUnit.equals("ngan") || normalizedUnit.equals("k")) {
                value = value.multiply(ONE_THOUSAND);
            }
            return normalizePrice(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String smartSummary(String smartCity, String smartKeyword, Integer smartRating, BigDecimal smartMaxPrice) {
        List<String> parts = new ArrayList<>();
        if (smartCity != null && !smartCity.isBlank()) {
            parts.add("khu vực " + smartCity);
        }
        if (smartKeyword != null && !smartKeyword.isBlank()) {
            parts.add("từ khóa " + smartKeyword);
        }
        if (smartRating != null) {
            parts.add("từ " + smartRating + " sao");
        }
        if (smartMaxPrice != null) {
            parts.add("dưới " + formatVnd(smartMaxPrice) + " VND/đêm");
        }
        return parts.isEmpty() ? "AI đã đọc yêu cầu, chưa tìm thấy điều kiện cụ thể để áp dụng."
                : "AI đã áp dụng: " + String.join(", ", parts) + ".";
    }

    private String formatVnd(BigDecimal value) {
        return String.format(Locale.US, "%,d", value.longValue());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
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

    private String fallbackImage(String city) {
        if (city == null) {
            return DEFAULT_THUMBNAIL;
        }
        String normalized = ascii(city);
        if (normalized.contains("ha noi") || normalized.contains("hanoi")) {
            return "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80";
        }
        if (normalized.contains("da nang") || normalized.contains("danang")) {
            return "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80";
        }
        if (normalized.contains("da lat") || normalized.contains("dalat")) {
            return "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1200&q=80";
        }
        if (normalized.contains("phu quoc")) {
            return "https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=1200&q=80";
        }
        if (normalized.contains("nha trang")) {
            return "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=1200&q=80";
        }
        if (normalized.contains("ho chi minh") || normalized.contains("saigon")) {
            return "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=1200&q=80";
        }
        return DEFAULT_THUMBNAIL;
    }

    private String googleMapsUrl(Hotel hotel) {
        if (hotel.getLatitude() != null && hotel.getLongitude() != null) {
            return "https://www.google.com/maps/search/?api=1&query="
                    + hotel.getLatitude() + "," + hotel.getLongitude();
        }
        String query = URLEncoder.encode(hotel.getName() + " " + hotel.getAddress(), StandardCharsets.UTF_8);
        return "https://www.google.com/maps/search/?api=1&query=" + query;
    }

    private String googleStaticMapUrl(Hotel hotel) {
        if (googleMapsApiKey.isBlank() || hotel.getLatitude() == null || hotel.getLongitude() == null) {
            return null;
        }
        String center = hotel.getLatitude() + "," + hotel.getLongitude();
        return "https://maps.googleapis.com/maps/api/staticmap?center=" + center
                + "&zoom=15&size=900x360&scale=2&maptype=roadmap&markers=color:red%7C"
                + center
                + "&key=" + URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8);
    }

    public record HotelCard(UUID id,
                            String name,
                            String city,
                            String address,
                            String description,
                            Integer rating,
                            String status,
                            String thumbnailUrl,
                            BigDecimal minPrice,
                            long roomCount,
                            BigDecimal latitude,
                            BigDecimal longitude,
                            String mapUrl,
                            String staticMapUrl) {
    }

    public record HotelDetail(HotelCard hotel, List<Room> rooms) {
    }

    public record HotelSearchResult(Page<HotelCard> hotels,
                                    String effectiveKeyword,
                                    String effectiveCity,
                                    Integer effectiveMinRating,
                                    BigDecimal effectiveMaxPrice,
                                    String smartSummary) {
    }

    private record HotelSearchFilters(String keyword,
                                      String city,
                                      Integer minRating,
                                      BigDecimal maxPrice,
                                      String smartSummary) {
    }
}
