# HotelBooking

Spring Boot hotel booking application prepared for deployment with PostgreSQL/Flyway, Brevo HTTP email/SMS API, and VNPay payment boundaries.

## Requirements

- Java 21
- Docker for local PostgreSQL or container deployment

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
- `BREVO_API_KEY`, `BREVO_SMS_SENDER`, `MAIL_FROM` for Brevo email & SMS transactional delivery

Do not commit real credentials. Use platform environment variables for all production secrets.

## Database And Migrations

Flyway migrations are in `src/main/resources/db/migration`. Production runs against PostgreSQL with `spring.jpa.hibernate.ddl-auto=none`.

```bash
./gradlew build
```

## Import Real Hotel Data

MVP source is OpenStreetMap through Overpass. Imported rows store source, external id, raw payload hash, import run logs, and source URLs.

```bash
./gradlew bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --app.import-hotels=true --app.import-hotels.exit=true --source=overpass --city=HCMC --limit=100"
```

Room/rate templates created for OSM places are marked `INTERNAL_TEMPLATE` and `INTERNAL_ESTIMATE`; they are not real provider availability or rates.

## Payment, Email, Refund

- Local/test: `MockPaymentProvider` and `ConsoleEmailProvider`.
- Production: mock providers are denied; Brevo HTTP APIs and a real payment adapter are required.
- Payment confirmation is webhook/IPN-driven with signature verification and idempotent event storage.
- Refund requests remain pending/processing unless a provider refund completes.

## Build

```bash
./gradlew clean build
```

## Health

```bash
curl http://localhost:8080/actuator/health
```

Actuator exposes `health`, `info`, and `metrics` when enabled by configuration.
