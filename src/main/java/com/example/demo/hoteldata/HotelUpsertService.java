package com.example.demo.hoteldata;

import com.example.demo.entity.Hotel;
import com.example.demo.entity.HotelSourceRecord;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomImage;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.HotelSourceRecordRepository;
import com.example.demo.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class HotelUpsertService {
    private final HotelRepository hotelRepository;
    private final HotelSourceRecordRepository sourceRecordRepository;
    private final RoomRepository roomRepository;
    private final DeduplicationService deduplicationService;
    private final DataQualityScoringService dataQualityScoringService;
    private final Clock clock;

    public HotelUpsertService(HotelRepository hotelRepository,
                              HotelSourceRecordRepository sourceRecordRepository,
                              RoomRepository roomRepository,
                              DeduplicationService deduplicationService,
                              DataQualityScoringService dataQualityScoringService,
                              Clock clock) {
        this.hotelRepository = hotelRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.roomRepository = roomRepository;
        this.deduplicationService = deduplicationService;
        this.dataQualityScoringService = dataQualityScoringService;
        this.clock = clock;
    }

    @Transactional
    public UpsertResult upsert(HotelDataRecord record, boolean dryRun) {
        if (!valid(record)) {
            return UpsertResult.skip();
        }
        Optional<Hotel> existingHotel = deduplicationService.findExisting(record);
        boolean existing = existingHotel.isPresent();
        if (dryRun) {
            return existing ? UpsertResult.update() : UpsertResult.insert();
        }

        Hotel hotel = existingHotel.orElseGet(Hotel::new);
        hotel.setName(record.name().trim());
        hotel.setSlug(slug(record.name()));
        hotel.setAddressLine(blankToFallback(record.addressLine(), record.city()));
        hotel.setAddress(blankToFallback(record.addressLine(), record.city()));
        hotel.setCity(record.city());
        hotel.setProvince(record.province());
        hotel.setCountry(record.country());
        hotel.setLatitude(record.latitude());
        hotel.setLongitude(record.longitude());
        hotel.setStarRating(record.starRating());
        hotel.setPhone(normalizePhone(record.phone()));
        hotel.setWebsite(normalizeWebsite(record.website()));
        hotel.setSource(record.source().toUpperCase(Locale.ROOT));
        hotel.setSourceExternalId(record.externalId());
        hotel.setSourceUrl(record.sourceUrl());
        if (record.primaryImageUrl() != null && !record.primaryImageUrl().isBlank()) {
            hotel.setThumbnailUrl(record.primaryImageUrl().trim());
        }
        hotel.setDataQualityScore(dataQualityScoringService.score(record));
        hotel.setDescription("Imported place data from " + record.source().toUpperCase(Locale.ROOT) + ". Availability and rates are internal estimates unless a provider offer is attached.");
        Instant now = Instant.now(clock);
        if (hotel.getImportedAt() == null) {
            hotel.setImportedAt(now);
        }
        hotel.setLastSyncedAt(now);
        Hotel saved = hotelRepository.save(hotel);
        upsertSourceRecord(record);
        ensureInternalRoomTemplate(saved, record, existing);
        return existing ? UpsertResult.update() : UpsertResult.insert();
    }

    private void upsertSourceRecord(HotelDataRecord record) {
        HotelSourceRecord sourceRecord = sourceRecordRepository
                .findBySourceAndExternalId(record.source().toUpperCase(Locale.ROOT), record.externalId())
                .orElseGet(HotelSourceRecord::new);
        sourceRecord.setSource(record.source().toUpperCase(Locale.ROOT));
        sourceRecord.setExternalId(record.externalId());
        sourceRecord.setRawPayload(record.rawPayload());
        sourceRecord.setPayloadHash(sha256(record.rawPayload()));
        sourceRecord.setLastSeenAt(Instant.now(clock));
        sourceRecordRepository.save(sourceRecord);
    }

    private void ensureInternalRoomTemplate(Hotel hotel, HotelDataRecord record, boolean existingHotel) {
        if (existingHotel) {
            Optional<Room> existingRoom = roomRepository.findFirstByHotelOrderByCreatedAtAsc(hotel);
            if (existingRoom.isPresent()) {
                updateInternalTemplateImage(existingRoom.get(), record);
                return;
            }
        }
        Room room = new Room();
        room.setHotel(hotel);
        room.setName("Standard Room");
        room.setRoomType("Standard");
        room.setCapacity(2);
        room.setAreaSqm(new BigDecimal("28"));
        room.setPricePerNight(estimateRate(hotel.getCity()));
        room.setDescription("Internal room template for imported hotel place data. This is not a provider room or live availability.");
        room.setCancellationPolicy("Internal estimate. Confirm policy with hotel/provider before production bookings.");
        room.setRoomSource("INTERNAL_TEMPLATE");
        room.setRateSource("INTERNAL_ESTIMATE");
        RoomImage image = new RoomImage();
        image.setRoom(room);
        image.setImageUrl(blankToFallback(record.primaryImageUrl(), getFallbackImageUrl(hotel.getCity())));
        image.setAltText(hotel.getName());
        image.setPrimary(true);
        room.getImages().add(image);
        roomRepository.save(room);
    }

    private void updateInternalTemplateImage(Room room, HotelDataRecord record) {
        if (record.primaryImageUrl() == null || record.primaryImageUrl().isBlank()) {
            return;
        }
        if (!"INTERNAL_TEMPLATE".equalsIgnoreCase(room.getRoomSource())) {
            return;
        }
        RoomImage image = room.getImages().stream()
                .filter(RoomImage::isPrimary)
                .findFirst()
                .orElseGet(() -> {
                    RoomImage created = new RoomImage();
                    created.setRoom(room);
                    created.setPrimary(true);
                    room.getImages().add(created);
                    return created;
                });
        image.setImageUrl(record.primaryImageUrl().trim());
        image.setAltText(room.getHotel().getName());
        roomRepository.save(room);
    }

    private BigDecimal estimateRate(String city) {
        String normalized = city == null ? "" : city.toLowerCase(Locale.ROOT);
        if (normalized.contains("phu quoc") || normalized.contains("da nang") || normalized.contains("nha trang")) {
            return new BigDecimal("1800000");
        }
        if (normalized.contains("ho chi minh") || normalized.contains("ha noi")) {
            return new BigDecimal("1500000");
        }
        return new BigDecimal("1100000");
    }

    private boolean valid(HotelDataRecord record) {
        return record.name() != null && !record.name().isBlank()
                && record.latitude() != null
                && record.longitude() != null
                && record.latitude().compareTo(new BigDecimal("-90")) >= 0
                && record.latitude().compareTo(new BigDecimal("90")) <= 0
                && record.longitude().compareTo(new BigDecimal("-180")) >= 0
                && record.longitude().compareTo(new BigDecimal("180")) <= 0;
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private String normalizeWebsite(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String trimmed = website.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "hotel" : normalized;
    }

    private String getFallbackImageUrl(String city) {
        if (city == null) return "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80";
        String normalized = city.toLowerCase(Locale.ROOT);
        if (normalized.contains("ha noi") || normalized.contains("hà nội")) {
            return "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80";
        } else if (normalized.contains("da nang") || normalized.contains("đà nẵng")) {
            return "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80";
        } else if (normalized.contains("da lat") || normalized.contains("đà lạt")) {
            return "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1200&q=80";
        } else if (normalized.contains("phu quoc") || normalized.contains("phú quốc")) {
            return "https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=1200&q=80";
        } else if (normalized.contains("nha trang")) {
            return "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=1200&q=80";
        } else if (normalized.contains("ho chi minh") || normalized.contains("hồ chí minh") || normalized.contains("saigon") || normalized.contains("sài gòn")) {
            return "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=1200&q=80";
        }
        return "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return "";
        }
    }

    public record UpsertResult(boolean inserted, boolean updated, boolean skipped) {
        static UpsertResult insert() {
            return new UpsertResult(true, false, false);
        }

        static UpsertResult update() {
            return new UpsertResult(false, true, false);
        }

        static UpsertResult skip() {
            return new UpsertResult(false, false, true);
        }
    }
}
