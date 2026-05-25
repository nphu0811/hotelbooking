package com.example.demo.hoteldata;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class HotelImportRunner implements ApplicationRunner {
    private final HotelImportService hotelImportService;
    private final ConfigurableApplicationContext applicationContext;

    public HotelImportRunner(HotelImportService hotelImportService,
                             ConfigurableApplicationContext applicationContext) {
        this.hotelImportService = hotelImportService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("app.import-hotels")) {
            return;
        }
        String source = option(args, "source", "overpass");
        String city = option(args, "city", "Ho Chi Minh City");
        int limit = Integer.parseInt(option(args, "limit", "100"));
        boolean dryRun = args.containsOption("dry-run");
        try {
            HotelImportResult result = hotelImportService.importHotels(new HotelImportRequest(source, city, limit, dryRun));
            System.out.printf("Hotel import complete: fetched=%d inserted=%d updated=%d skipped=%d%n",
                    result.totalFetched(), result.totalInserted(), result.totalUpdated(), result.totalSkipped());
            if (args.containsOption("app.import-hotels.exit")) {
                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            }
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "unknown error" : hotelImportService.sanitize(ex.getMessage());
            System.err.printf("Hotel import failed: %s%n", message);
            if (args.containsOption("app.import-hotels.exit")) {
                int exitCode = SpringApplication.exit(applicationContext, () -> 1);
                System.exit(exitCode);
            }
        }
    }

    private String option(ApplicationArguments args, String name, String fallback) {
        if (!args.containsOption(name) || args.getOptionValues(name).isEmpty()) {
            return fallback;
        }
        return args.getOptionValues(name).get(0);
    }
}
