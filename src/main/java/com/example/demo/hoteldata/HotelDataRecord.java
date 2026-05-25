package com.example.demo.hoteldata;

import java.math.BigDecimal;
import java.util.Map;

public record HotelDataRecord(
        String source,
        String externalId,
        String name,
        String addressLine,
        String city,
        String province,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer starRating,
        String phone,
        String website,
        String sourceUrl,
        String primaryImageUrl,
        String imageAttribution,
        Map<String, String> amenities,
        String rawPayload
) {
}
