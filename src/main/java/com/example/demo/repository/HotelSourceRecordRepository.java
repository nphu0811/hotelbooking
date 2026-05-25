package com.example.demo.repository;

import com.example.demo.entity.HotelSourceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HotelSourceRecordRepository extends JpaRepository<HotelSourceRecord, UUID> {
    Optional<HotelSourceRecord> findBySourceAndExternalId(String source, String externalId);
}
