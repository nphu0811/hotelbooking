package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hotel_import_runs")
public class HotelImportRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "import_run_id")
    private UUID id;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false, length = 40)
    private String status = "RUNNING";

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant finishedAt;

    private int totalFetched;
    private int totalInserted;
    private int totalUpdated;
    private int totalSkipped;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public UUID getId() {
        return id;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public void setTotalFetched(int totalFetched) {
        this.totalFetched = totalFetched;
    }

    public void setTotalInserted(int totalInserted) {
        this.totalInserted = totalInserted;
    }

    public void setTotalUpdated(int totalUpdated) {
        this.totalUpdated = totalUpdated;
    }

    public void setTotalSkipped(int totalSkipped) {
        this.totalSkipped = totalSkipped;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
