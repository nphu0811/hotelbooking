package com.example.demo.hoteldata;

import org.springframework.stereotype.Service;

@Service
public class DataQualityScoringService {
    public int score(HotelDataRecord record) {
        int score = 30;
        if (notBlank(record.name())) score += 20;
        if (notBlank(record.addressLine())) score += 10;
        if (record.latitude() != null && record.longitude() != null) score += 20;
        if (notBlank(record.phone())) score += 5;
        if (notBlank(record.website())) score += 5;
        if (record.starRating() != null) score += 5;
        if (record.amenities() != null && !record.amenities().isEmpty()) score += 5;
        return Math.min(score, 100);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
