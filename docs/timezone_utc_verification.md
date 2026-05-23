# Timezone UTC Verification

## Configuration

- `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`: configured.
- `spring.jackson.time-zone=UTC`: configured.
- Schema timestamp columns use `TIMESTAMPTZ`.

## DB Timezone

- Status: VERIFIED
- Query: `SHOW timezone;`
- Result: `UTC`

## App Timestamp Serialization

- Status: VERIFIED
- Result: application logs print UTC `Z` timestamps after `logging.pattern.dateformat` update.

## Conclusion

UTC is fully configured end-to-end and verified on both application logs and live Railway PostgreSQL database.
