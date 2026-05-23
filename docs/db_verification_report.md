# Database Verification Report

## Local Migration

- Status: PARTIAL
- Command: `.\gradlew.bat build`
- Result: PASS using H2 local/test schema generation. PostgreSQL Flyway SQL files are present but not applied to a live PostgreSQL instance in this run.

## Railway Migration

- Status: BLOCKED
- Reason: Railway credential must be supplied through environment variables, and the previously exposed credential should be rotated before trusted verification.

## Verification Queries

- Script: `scripts/db/check_railway_db.sql`
- Status: PENDING for live PostgreSQL/Railway.
