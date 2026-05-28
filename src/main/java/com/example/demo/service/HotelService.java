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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class HotelService {
    private static final String DEFAULT_THUMBNAIL =
            "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80";

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
        return searchHotels(keyword, city, minRating, PageRequest.of(Math.max(page, 0), 20, defaultSort()));
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> searchHotels(String keyword, String city, Integer minRating, Pageable pageable) {
        String safeKeyword = clean(keyword);
        String safeCity = clean(city);
        Page<Hotel> hotels = hotelRepository.searchActive(
                safeKeyword,
                ascii(safeKeyword),
                safeCity,
                ascii(safeCity),
                minRating,
                pageable);
        List<HotelCard> cards = hotels.getContent().stream()
                .map(this::toCard)
                .toList();
        return new PageImpl<>(cards, pageable, hotels.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<HotelCard> featuredHotels(int size) {
        return searchHotels("", "", null, PageRequest.of(0, Math.max(size, 1), defaultSort()));
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
}
