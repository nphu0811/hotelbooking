package com.example.demo.hoteldata;

import com.example.demo.service.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmadeusHotelDataProvider implements HotelDataProvider {
    private final String clientId;
    private final String clientSecret;

    public AmadeusHotelDataProvider(@Value("${amadeus.client-id:}") String clientId,
                                    @Value("${amadeus.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getSource() {
        return "amadeus";
    }

    @Override
    public List<HotelDataRecord> fetch(HotelImportRequest request) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new BusinessException("Amadeus credentials are required");
        }
        throw new BusinessException("Amadeus import adapter skeleton is present; production hotel offers need credentials and terms review");
    }
}
