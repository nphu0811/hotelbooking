# API Keys Setup

No real key should be committed. Use `.env.example` as the variable list and inject real values through the shell, IDE run configuration, Railway variables, or CI secrets.

## Railway PostgreSQL

- Purpose: production PostgreSQL database.
- Register: https://railway.com/
- Free tier: check current Railway plan before relying on free usage.
- Variables: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`; alternatively `DATABASE_URL` or `DATABASE_PUBLIC_URL`.
- Test: run app with env vars, then run `scripts/db/check_railway_db.sql`.
- Fallback: local profile uses H2 with synthetic seed data.

## Hotel data import (Overpass / Geoapify / Google Places)

- MVP default: Overpass only (`GEOAPIFY_ENABLED=false`, `GOOGLE_PLACES_ENABLED=false`).
- Geoapify optional enrichment: register at https://www.geoapify.com/, set `GEOAPIFY_API_KEY`, enable with `GEOAPIFY_ENABLED=true`. See `docs/geoapify-setup.md`.
- Google Places optional enrichment: set `GOOGLE_PLACES_API_KEY` and `GOOGLE_PLACES_ENABLED=true` only when billing/API access is available. See `docs/hotel-data-import-google-places.md`.
- Overpass: optional `HOTELDATA_OVERPASS_ENDPOINT` (defaults to public instance).

## Maps

- Purpose: map display and future geocoding.
- Preferred free stack: Leaflet + OpenStreetMap tiles/data.
- Register: no API key for basic Leaflet/OSM display; respect tile usage policies and attribution.
- Variables: `MAPS_PROVIDER=leaflet`; optional `GOOGLE_MAPS_API_KEY`.
- Test: open room detail and verify map/location placeholder or Leaflet integration.
- Fallback: show address text only.

## Payment

- Purpose: online payment.
- Preferred implementation now: `MOCK` sandbox only, no real money.
- VNPay sandbox: https://sandbox.vnpayment.vn/
- VNPay variables: `VNPAY_*`.
- MoMo is not enabled in production until a real adapter and sandbox verification are added.
- Test: use `/payments/mock/...` success/failure/invalid signature flows.
- Fallback: mock payment provider remains enabled for dev/test.

## Email

- Purpose: verification, booking confirmation, cancellation, check-in/out, review request.
- Options: SMTP Gmail App Password, SendGrid, Mailgun.
- Variables: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Test: local app currently queues and logs mock sends. Real SMTP should be tested in a non-production mailbox.
- Fallback: email jobs/logs are stored without sending real email.

## Image Upload

- Purpose: room and review image storage.
- Option: Cloudinary free tier.
- Variables: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Test: upload room image in admin flow once integration is enabled.
- Fallback: local SVG placeholders in `src/main/resources/static/css/`.

## CAPTCHA

- Purpose: repeated login failure protection.
- Options: Cloudflare Turnstile or Google reCAPTCHA.
- Variables: reserve `CAPTCHA_SITE_KEY`, `CAPTCHA_SECRET` for a real third-party challenge.
- Test: after 3 failed login attempts, the UI shows a CAPTCHA hook; after 5 failed attempts in 15 minutes, the account is locked temporarily.
- Fallback: current app enforces the lockout and visible hook; third-party challenge verification is pending until real keys are configured.
