package com.example.demo.hoteldata;

import java.util.List;

public interface HotelDataProvider {
    String getSource();

    List<HotelDataRecord> fetch(HotelImportRequest request);
}
