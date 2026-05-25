package com.example.demo.hoteldata;

public record HotelImportResult(
        int totalFetched,
        int totalInserted,
        int totalUpdated,
        int totalSkipped
) {
}
