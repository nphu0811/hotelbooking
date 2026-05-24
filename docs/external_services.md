# External Services

## Current Implementation

- Database: PostgreSQL schema via Flyway; local dev/test uses H2.
- Payment: mock provider only. It supports success, failure, duplicate callback idempotency, and invalid signature rejection path.
- Email: queue/log service with mock successful sends. Real SMTP is not enabled by default.
- Images: local SVG placeholders.
- Maps: address text only; Leaflet/OpenStreetMap is the preferred next implementation.
- CAPTCHA: local login failure tracking shows a visible hook after 3 failures and locks accounts after 5 failures. Third-party challenge verification is reserved for real keys.

## Data Source

- OpenStreetMap/Overpass is the preferred open data source for hotel/accommodation seed extraction.
- License: OpenStreetMap data is licensed under ODbL and requires attribution.
- Local fallback: synthetic seed data is included so the app can run offline.

## Security Posture

- No real Railway URL or service key is committed.
- Real payment gateways must use sandbox credentials first.
- Production verification must run only after rotating the previously exposed Railway credential.
