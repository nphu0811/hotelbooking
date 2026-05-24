# Timezone UTC Verification

## Configuration

- `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`: configured in main, local, and test runtime properties.
- `spring.jackson.time-zone=UTC`: configured in main and test runtime properties.
- `logging.pattern.dateformat=yyyy-MM-dd'T'HH:mm:ss.SSSXXX,UTC`: configured so Spring logs render UTC `Z`.
- PostgreSQL Flyway schema uses `TIMESTAMPTZ` for timestamp columns.
- Application startup sets JVM default timezone to UTC in `HotelBookingApplication`.

## Verified Locally

- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: test shutdown logs printed UTC timestamps such as `2026-05-23T16:54:52.861Z`.
- Command: `.\gradlew.bat test --rerun-tasks`
- Result: PASS
- Evidence: `jacksonSerializesInstantAsUtcText()` confirms app Jackson serialization emits `2026-05-23T18:00:00Z`.

## Live PostgreSQL Verification

- Status: VERIFIED
- Query: `SHOW timezone;`
- Result: `Etc/UTC`
- Environment: Railway PostgreSQL via existing local `.env` variables.

## Conclusion

Application-side UTC handling is configured and locally verified. Railway PostgreSQL also reports UTC-compatible timezone `Etc/UTC`.
