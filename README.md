# HotelBooking

Spring Boot hotel booking application hardened for production-style deployment, payment/email provider boundaries, and legal hotel place-data import.

## Requirements

- Java 21
- Node.js 20+ for Playwright smoke tests
- Docker for local PostgreSQL or CI-like database checks

## Local Setup

```bash
cp .env.example .env
docker compose up -d postgres
./gradlew bootRun --args="--spring.profiles.active=local"
```

Local profile uses H2, console email, and mock payment only for development/testing. Production profile rejects H2, mock payment/email, seed data, and placeholder secrets.

## Production Profile

Required environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_PAYMENT_PROVIDER`
- `APP_PUBLIC_BASE_URL` public HTTPS base URL used in email links and provider redirects
- `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`, `VNPAY_IPN_URL` when `APP_PAYMENT_PROVIDER=vnpay`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`

See `docs/production-profile.md` and `docs/security-rotation-checklist.md`.

## Database And Migrations

Flyway migrations are in `src/main/resources/db/migration`. Production runs against PostgreSQL with `spring.jpa.hibernate.ddl-auto=none`.

```bash
./gradlew test
./gradlew build
```

The Testcontainers migration test runs when Docker is available; otherwise it is skipped by JUnit.

## Import Real Hotel Data

MVP source is OpenStreetMap through Overpass. Imported rows store source, external id, raw payload hash, import run logs, and source URLs.

```bash
./gradlew bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --app.import-hotels=true --app.import-hotels.exit=true --source=overpass --city=HCMC --limit=100"
```

Room/rate templates created for OSM places are marked `INTERNAL_TEMPLATE` and `INTERNAL_ESTIMATE`; they are not real provider availability or rates.

## Payment, Email, Refund

- Local/test: `MockPaymentProvider` and `ConsoleEmailProvider`.
- Production: mock providers are denied; SMTP and a real payment adapter are required.
- Payment confirmation is webhook/IPN-driven with signature verification and idempotent event storage.
- Refund requests remain pending/processing unless a provider refund completes.

See `docs/payment-email-refund.md`.

## Tests

```bash
./gradlew clean test
./gradlew build
npm install
npx playwright install chromium
npm run test:e2e -- --reporter=line --workers=1
```

## Health And Operations

```bash
curl http://localhost:8080/actuator/health
```

Actuator exposes `health`, `info`, and `metrics`. See `docs/deployment-checklist.md` for deployment, smoke checks, backup notes, and data import procedure.
