package com.example.demo.hoteldata;

import com.example.demo.entity.Hotel;
import com.example.demo.repository.HotelRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class DeduplicationService {
    private static final BigDecimal COORDINATE_WINDOW = new BigDecimal("0.0007");

    private final HotelRepository hotelRepository;

    public DeduplicationService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public Optional<Hotel> findExisting(HotelDataRecord record) {
        Optional<Hotel> byExternalId = hotelRepository.findBySourceAndSourceExternalId(record.source().toUpperCase(java.util.Locale.ROOT), record.externalId());
        if (byExternalId.isPresent()) {
            return byExternalId;
        }
        if (record.latitude() == null || record.longitude() == null || record.name() == null) {
            return Optional.empty();
        }
        BigDecimal minLat = record.latitude().subtract(COORDINATE_WINDOW);
        BigDecimal maxLat = record.latitude().add(COORDINATE_WINDOW);
        BigDecimal minLng = record.longitude().subtract(COORDINATE_WINDOW);
        BigDecimal maxLng = record.longitude().add(COORDINATE_WINDOW);
        if (record.source().equalsIgnoreCase("geoapify")) {
            Optional<Hotel> osmHotel = hotelRepository.findNearbyByNameAndSource(
                    record.name(),
                    "OVERPASS",
                    minLat,
                    maxLat,
                    minLng,
                    maxLng
            );
            if (osmHotel.isPresent()) {
                return osmHotel;
            }
        }
        return hotelRepository.findNearbyByName(
                record.name(),
                minLat,
                maxLat,
                minLng,
                maxLng
        );
    }
}
