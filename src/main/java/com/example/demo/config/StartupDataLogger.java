package com.example.demo.config;

import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupDataLogger {
    private static final Logger log = LoggerFactory.getLogger(StartupDataLogger.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final Environment environment;

    public StartupDataLogger(HotelRepository hotelRepository,
                             RoomRepository roomRepository,
                             Environment environment) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logInventorySnapshot() {
        long hotels = hotelRepository.countByDeletedFalse();
        long rooms = roomRepository.countByDeletedFalse();
        long activeRooms = roomRepository.countByStatusAndDeletedFalse(
                com.example.demo.entity.RoomStatus.AVAILABLE);
        String datasource = redactJdbcUrl(environment.getProperty("spring.datasource.url", "not configured"));
        log.info("Startup inventory: hotels={}, rooms={} (available={}), datasource={}",
                hotels, rooms, activeRooms, datasource);
    }

    private static String redactJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "not configured";
        }
        return url.replaceAll("(?i)(password=)[^&;]+", "$1***")
                .replaceAll("(?i)(:[^/@:]+)@/", ":***@/");
    }
}
