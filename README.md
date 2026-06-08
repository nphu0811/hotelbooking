# LUMIERE Hotel Booking

Premium hotel booking web application built with Spring Boot, Thymeleaf, Spring Security, Flyway, and provider-ready payment/email integrations.

The app supports guest search, room details, secure authentication, OTP verification, booking checkout, mock/local payment flows, production payment boundaries, admin management, and AI-assisted room recommendations.

## Highlights

- Clean LUMIERE-style responsive UI with light/dark theme support.
- Password login, OTP login, Google/Facebook OAuth entry points, email and phone verification.
- Hotel and room browsing with search, detail pages, booking history, cancellation and refund states.
- Admin dashboard for rooms, bookings, users, refunds, and operational review.
- Payment abstraction with local mock payment plus VNPay and MoMo provider adapters.
- Transactional email/SMS abstraction with local console providers and Brevo-ready production delivery.
- Flyway-managed PostgreSQL schema for production; H2 in-memory profile for fast local development.
- AI recommendation endpoint powered by `OPENAI_API_KEY` when configured.
- E2E fixture mode and Playwright smoke coverage for authenticated booking flows.

## Tech Stack

| Area | Tools |
| --- | --- |
| Backend | Java 21, Spring Boot 4, Spring MVC, Spring Security |
| Views | Thymeleaf, CSS, vanilla JavaScript |
| Data | Spring Data JPA, Flyway, PostgreSQL, H2 local profile |
| Auth | Form login, OTP, OAuth2 client |
| Payments | Mock, VNPay, MoMo adapters |
| Messaging | Console providers, Brevo email/SMS integration |
| Testing | JUnit, Spring tests, Testcontainers, Playwright |
| Runtime | Gradle, Docker Compose |

## Quick Start

### 1. Clone and prepare environment

```bash
git clone https://github.com/nphu0811/hotelbooking.git
cd hotelbooking
cp .env.example .env
```

### 2. Run locally with the development profile

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

Open:

```text
http://localhost:8080
```

The `local` profile uses:

- H2 in-memory database
- seeded demo data
- console email/SMS providers
- mock payment provider
- H2 console enabled for development

## Useful Commands

```bash
# Run backend tests
./gradlew test

# Build the application jar
./gradlew bootJar

# Full build
./gradlew clean build

# Run Playwright smoke tests
npm install
npm run test:e2e
```

## Docker

Start PostgreSQL for local infrastructure work:

```bash
docker compose up -d postgres
```

The `app` service in `docker-compose.yml` is wired for production-like settings and requires real environment variables for public URL, payment provider, and Brevo credentials.

## Production Configuration

Set the production profile and provide secrets through environment variables. Do not commit real credentials.

Required baseline variables:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_PUBLIC_BASE_URL
APP_PAYMENT_PROVIDER
APP_EMAIL_PROVIDER
APP_SMS_PROVIDER
MAIL_FROM
```

VNPay variables when `APP_PAYMENT_PROVIDER=vnpay`:

```text
VNPAY_TMN_CODE
VNPAY_HASH_SECRET
VNPAY_PAY_URL
VNPAY_RETURN_URL
VNPAY_IPN_URL
```

MoMo variables when `APP_PAYMENT_PROVIDER=momo`:

```text
MOMO_PARTNER_CODE
MOMO_ACCESS_KEY
MOMO_SECRET_KEY
MOMO_CREATE_URL
MOMO_RETURN_URL
MOMO_IPN_URL
```

Brevo variables for production email/SMS:

```text
BREVO_API_KEY
BREVO_SMS_SENDER
```

Optional integrations:

```text
OPENAI_API_KEY
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
FACEBOOK_CLIENT_ID
FACEBOOK_CLIENT_SECRET
GOOGLE_PLACES_API_KEY
GEOAPIFY_API_KEY
AMADEUS_CLIENT_ID
AMADEUS_CLIENT_SECRET
```

Production guardrails reject unsafe combinations such as H2, mock payment/email providers, demo seed data, and placeholder secrets.

## Database And Migrations

Flyway migrations live in:

```text
src/main/resources/db/migration
```

Production uses PostgreSQL with:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Local development uses H2 with schema recreation for quick iteration.

## Import Real Hotel Data

The import runner can pull hotel source records from Overpass/OpenStreetMap.

```bash
./gradlew bootRun --args="--spring.profiles.active=local --spring.main.web-application-type=none --app.import-hotels=true --app.import-hotels.exit=true --source=overpass --city=HCMC --limit=100"
```

Imported records store source metadata, external IDs, raw payload hashes, import run logs, and source URLs.

Room/rate templates generated for imported OSM places are marked as internal templates or estimates; they are not live provider inventory.

## Health Checks

```bash
curl http://localhost:8080/actuator/health
```

Actuator exposes health, info, and metrics endpoints when enabled by configuration.

## Project Layout

```text
src/main/java/com/example/demo
  config/        security, providers, startup guards
  controller/    web routes and admin flows
  entity/        JPA domain models
  payment/       payment provider adapters
  repository/    Spring Data repositories
  service/       business logic

src/main/resources
  templates/     Thymeleaf views
  static/        CSS, JavaScript, images
  db/migration/  Flyway migrations

src/test
  java/          backend tests
  e2e/           Playwright smoke tests
```

## Notes

- Keep all production secrets in environment variables.
- Use `local` only for development and testing.
- Payment confirmation is webhook/IPN-driven with signature verification and idempotent event storage.
- Refund requests remain pending or processing until the selected payment provider confirms settlement.
