package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hotel_source_records")
public class HotelSourceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "source_record_id")
    private UUID id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false, length = 120)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String rawPayload;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Column(nullable = false)
    private Instant importedAt = Instant.now();

    @Column(nullable = false)
    private Instant lastSeenAt = Instant.now();

    public void setSource(String source) {
        this.source = source;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
