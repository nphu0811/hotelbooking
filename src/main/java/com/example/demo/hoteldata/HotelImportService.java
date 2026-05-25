package com.example.demo.hoteldata;

import com.example.demo.entity.HotelImportRun;
import com.example.demo.repository.HotelImportRunRepository;
import com.example.demo.service.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class HotelImportService {
    private final HotelDataProviderRegistry providerRegistry;
    private final HotelUpsertService hotelUpsertService;
    private final HotelImportRunRepository importRunRepository;
    private final Clock clock;
    private final String googleApiKey;
    private final String geoapifyApiKey;

    public HotelImportService(HotelDataProviderRegistry providerRegistry,
                              HotelUpsertService hotelUpsertService,
                              HotelImportRunRepository importRunRepository,
                              Clock clock,
                              @Value("${google.places.api-key:}") String googleApiKey,
                              @Value("${geoapify.api-key:}") String geoapifyApiKey) {
        this.providerRegistry = providerRegistry;
        this.hotelUpsertService = hotelUpsertService;
        this.importRunRepository = importRunRepository;
        this.clock = clock;
        this.googleApiKey = googleApiKey == null ? "" : googleApiKey.trim();
        this.geoapifyApiKey = geoapifyApiKey == null ? "" : geoapifyApiKey.trim();
    }

    public HotelImportResult importHotels(HotelImportRequest request) {
        HotelImportRun run = new HotelImportRun();
        run.setSource(request.source().toUpperCase());
        run.setStatus(request.dryRun() ? "DRY_RUN" : "RUNNING");
        importRunRepository.save(run);

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        try {
            List<HotelDataRecord> fetched = providerRegistry.require(request.source()).fetch(request);
            int limit = request.limit() <= 0 ? fetched.size() : Math.min(request.limit(), fetched.size());
            for (HotelDataRecord record : fetched.subList(0, limit)) {
                HotelUpsertService.UpsertResult result = hotelUpsertService.upsert(record, request.dryRun());
                if (result.inserted()) inserted++;
                if (result.updated()) updated++;
                if (result.skipped()) skipped++;
            }
            run.setTotalFetched(fetched.size());
            run.setTotalInserted(inserted);
            run.setTotalUpdated(updated);
            run.setTotalSkipped(skipped);
            run.setStatus(request.dryRun() ? "DRY_RUN" : "SUCCEEDED");
            run.setFinishedAt(Instant.now(clock));
            importRunRepository.save(run);
            return new HotelImportResult(fetched.size(), inserted, updated, skipped);
        } catch (RuntimeException ex) {
            run.setStatus("FAILED");
            String sanitizedMsg = sanitize(ex.getMessage());
            run.setErrorMessage(sanitizedMsg);
            run.setFinishedAt(Instant.now(clock));
            importRunRepository.save(run);
            throw new BusinessException(sanitizedMsg, ex);
        }
    }

    public String sanitize(String message) {
        if (message == null) {
            return null;
        }
        String sanitized = message;
        if (!googleApiKey.isBlank()) {
            sanitized = sanitized.replace(googleApiKey, "[REDACTED]");
        }
        if (!geoapifyApiKey.isBlank()) {
            sanitized = sanitized.replace(geoapifyApiKey, "[REDACTED]");
        }
        sanitized = sanitized.replaceAll("(?i)(key|api_key|apikey|token)=[a-zA-Z0-9_-]+", "$1=[REDACTED]");
        sanitized = sanitized.replaceAll("(?i)(key|api_key|apikey|token)\\s+[a-zA-Z0-9_-]+", "$1 [REDACTED]");
        sanitized = sanitized.replaceAll("(?i)\\b[a-f0-9]{32}\\b", "[REDACTED]");
        return sanitized;
    }
}
