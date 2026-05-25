package com.example.demo.hoteldata;

public record HotelImportRequest(
        String source,
        String city,
        int limit,
        boolean dryRun
) {
}
