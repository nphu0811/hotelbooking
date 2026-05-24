# Railway Verification Log

## 2026-05-23 UTC - Existing Railway Database Accepted By User

- Status: VERIFIED
- Credential handling: used existing local `.env` Railway variables for the process only. Secret values were not printed and were not written to docs.
- User decision: continue using the existing Railway database/credential and do not require rotation for this task.
- App target: local Spring Boot app on `http://localhost:8080` backed by Railway PostgreSQL.

## Database Verification

- JDBC connection: PASS
- `SHOW timezone;`: `Etc/UTC`
- Flyway: PASS
  - V1 `init schema`: success
  - V2 `indexes constraints`: success
- Tables in public schema: 19
- Indexes in public schema: 61
- Constraints in public schema: 211
- Double-booking exclusion constraint: `ex_bookings_no_overlap` present

## Dataset Counts Before App Write

- Hotels: 3
- Rooms: 5
- Bookings: 0
- Payments: 0
- Reviews: 0

## App Read/Write Verification

- Home: 200
- Search: 200
- Room detail: 200
- Guest booking attempt: redirected to login
- Customer login: 200
- Create booking checkout: 200
- Mock payment page: 200
- Mock payment success callback: 200
- Booking history contains new booking: true
- Admin dashboard: 200

## Railway Write Confirmation

- Booking ID: `28eeb724-a5c7-4ee6-add2-3b2f59874780`
- Booking status in Railway: `CONFIRMED`
- Payment status in Railway: `SUCCESS`
- Payment provider: `MOCK`

## Dataset Counts After App Write

- Hotels: 3
- Rooms: 5
- Bookings: 1
- Payments: 1
- Reviews: 0
- Email jobs: 1

## Notes

- `railway` CLI and `psql` were not installed in PATH, so verification used a temporary Java JDBC verifier with the Gradle-cached PostgreSQL JDBC driver.
- The verifier temporary source/class files were removed after execution.
