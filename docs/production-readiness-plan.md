# Production Readiness Plan

Last updated: 2026-05-25

## 1. Blockers From Prior Review

- Public demo credentials are exposed in the login UI and `DataSeeder` creates demo customer/admin users automatically.
- `DataSeeder` is enabled for the `default` profile and can create privileged accounts outside an explicitly local/test run.
- Production configuration is incomplete: datasource, mail, payment, and external service properties have unsafe fallbacks or placeholder values.
- `/h2-console/**` is publicly permitted in the security chain and frame options are globally relaxed.
- Mock payment is user-facing: payment completion uses a GET callback, public query parameters, and a hard-coded `mock-secret`.
- Email jobs are marked `SENT` without calling a real provider.
- Refund flow changes local state before provider refund confirmation and has no provider reconciliation.
- Payment, email, and refund flows lack provider abstractions, webhook/IPN signature verification, raw webhook storage, reconciliation, and production fail-fast checks.
- Hotel data is seeded as synthetic/demo data and is not a legally sourced real-data import pipeline with provenance.
- PostgreSQL/Flyway exists, but production-grade migration verification, Testcontainers, import-run logging, and source-record provenance are incomplete.
- Admin booking/user/room queries use broad entity loading and need pagination/N+1 review before large real datasets.
- Auth/security hardening is incomplete: plaintext verification token storage, resend cooldown, stronger validation DTOs, rate limiting, and IDOR coverage need work.
- Frontend polish is not enough for production by itself; shared layouts, source attribution, accessibility, empty/error states, and browser smoke tests remain required.
- DevOps/observability is incomplete: no production Dockerfile, local PostgreSQL compose, CI workflow, health/metrics setup, or deployment checklist.

## 2. Phase Checklist

### Phase 0 - Security Containment First

- [x] Create this production readiness plan.
- [x] Remove public demo credentials and prefilled demo login values.
- [x] Gate demo seeding behind `app.seed-demo-data=true` and local/dev/test profile only.
- [x] Remove automatic demo customer/admin creation from main runtime seeding.
- [x] Add production profile with PostgreSQL-only, no H2 console, no demo seed, no mock payment/email, and required env vars.
- [x] Add fail-fast validation for missing/placeholder/mock production secrets.
- [x] Add security rotation checklist.
- [x] Lock debug surfaces and deny H2 console/mock payment paths outside local/dev/test.
- [x] Add production security headers and cookie settings.
- [x] Add tests proving production fail-fast and H2/mock endpoint denial.
- [x] Run Phase 0 verification commands and fix failures.

### Phase 1 - Payment, Email, Refund Real Architecture

- [x] Add `PaymentProvider` abstraction.
- [x] Keep `MockPaymentProvider` local/dev/test only.
- [x] Add real provider adapter skeleton for VNPay.
- [x] Replace local mock callback with POST-only local flow; production uses POST webhook/IPN endpoint.
- [x] Verify HMAC/signature, amount, currency, order ID, booking ID, and provider transaction ID in provider webhook flow.
- [x] Store raw webhook payloads in `payment_webhook_events`.
- [x] Add webhook idempotency.
- [ ] Add scheduled payment reconciliation job.
- [x] Add `EmailProvider` abstraction with SMTP and local console providers.
- [x] Add retry/backoff and provider message ID logging without token leakage.
- [x] Implement refund request/provider handoff workflow.
- [ ] Complete provider-specific refund reconciliation.
- [x] Add migrations, tests, and docs.

### Phase 2 - Real Hotel Data + API Integration

- [x] Research legal/current API/source options using official docs where possible.
- [x] Create `docs/hotel-data-source-decision.md`.
- [x] Add schema for hotel provenance, source records, images, amenities, import runs, and rate-limit state as needed.
- [x] Add `HotelDataProvider` interface and provider adapters.
- [x] Implement Overpass import as the no-key legal MVP source if terms/rate limits remain suitable.
- [x] Implement optional Google Places/Amadeus/provider skeletons gated by env vars and terms.
- [x] Add idempotent upsert, deduplication, validation, normalization, data-quality scoring, dry-run, and limit mode.
- [x] Add command/script to import hotels.
- [x] Run a legal import of 20-100 hotels if the selected source does not require paid credentials.

### Phase 3 - Database, Query, Performance

- [x] Add/verify indexes for city/province, price, coordinates, source/external IDs, booking overlaps, and statuses.
- [x] Add database constraints for normalized unique email, statuses, non-null fields, and foreign keys.
- [x] Refactor admin/user/booking queries to avoid N+1 and entity-graph pagination issues.
- [x] Add pagination/filter/sort for search at production scale.
- [x] Add Testcontainers PostgreSQL migration tests.
- [x] Document query decisions in `docs/database-performance.md`.

### Phase 4 - Auth, Validation, Security Hardening

- [x] Add Bean Validation DTO/form models.
- [x] Normalize email with `Locale.ROOT` everywhere.
- [x] Hash verification tokens and avoid plaintext token storage.
- [x] Add resend cooldown and IP/email/user rate limiting.
- [x] Add account enumeration protection.
- [x] Add CSRF consistency and remove duplicate CSRF inputs if found.
- [x] Add IDOR and role-based access tests.
- [x] Add secure error pages with no stack trace/secret leakage.
- [x] Inject `Clock` for business-time logic.

### Phase 5 - Frontend/UI/UX Production Polish

- [x] Add shared Thymeleaf fragments for head, navbar, footer, scripts, and alerts.
- [ ] Remove duplicated head/CSS links across every legacy admin/booking template.
- [x] Add empty and error states.
- [x] Improve search/detail cards with real data, image fallback, address, amenities, source, and map link.
- [x] Add accessible labels, aria where needed, keyboard navigation, and visible focus state.
- [x] Add responsive mobile layout verification.
- [x] Add data source attribution.
- [x] Add Playwright smoke tests for desktop and mobile Chromium.

### Phase 6 - DevOps, CI/CD, Observability

- [x] Add production-grade `README.md`.
- [x] Update `.env.example` without real secrets.
- [x] Add production Dockerfile.
- [x] Add local PostgreSQL `docker-compose.yml`.
- [x] Add GitHub Actions CI for build, unit tests, integration tests, and optional Playwright tests.
- [x] Add health endpoint, structured logging, and Actuator metrics.
- [x] Add PostgreSQL/Railway backup and restore notes.
- [x] Add deployment checklist.

## 3. Risk Register

| Risk | Severity | Status | Mitigation |
| --- | --- | --- | --- |
| Previously exposed Railway/PostgreSQL credential may still be valid | Critical | Open | Rotate credential, revoke old value, update Railway variables, rerun migrations and smoke tests. |
| Demo admin creation outside local/test | Critical | Contained | Seeder is local/dev/test plus explicit flag, and main seeder no longer creates users/admins. |
| Public mock payment can confirm bookings | Critical | Contained | Mock endpoints are local/dev/test only and denied by production debug-surface filter; full provider webhook replacement remains Phase 1. |
| Email jobs falsely marked sent | High | Contained | SMTP/console providers now determine sent state and provider message IDs are logged. |
| Refund status can diverge from real money movement | High | Partially contained | Refund handoff states exist, but provider-specific reconciliation remains a limitation until real provider credentials/webhooks are configured. |
| Synthetic hotel data can be mistaken for real inventory | High | Contained | Imported hotels carry provenance, and generated rooms/rates are marked `INTERNAL_TEMPLATE` and `INTERNAL_ESTIMATE`. |
| API provider terms can restrict caching/storage | High | Contained for MVP | OSM/Overpass is selected for MVP place data; Google/Amadeus/Expedia remain credential/terms-gated. |
| Entity graph pagination and N+1 issues at scale | Medium | Contained | Search uses ID paging plus fetch graph; admin booking page fetches detailed graph. |
| Production deployment not reproducible | Medium | Contained | README, Dockerfile, compose, CI, actuator health/metrics, and deployment checklist were added. |

## 4. Verification Commands By Phase

### Phase 0

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat test --tests "*Production*"
```

Additional checks:

```powershell
rg -n "customer@example\.test|admin@example\.test|User@123|Admin@123|mock-secret" src/main
rg -n "/h2-console|app.seed-demo-data|spring.profiles.active|payment.provider|email.provider" src/main/resources src/test/resources
```

### Phase 1

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat test --tests "*Payment*" --tests "*Email*" --tests "*Refund*"
```

### Phase 2

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat bootRun --args='--spring.profiles.active=local --spring.main.web-application-type=none --app.import-hotels=true --app.import-hotels.exit=true --source=overpass --city=HCMC --limit=100'
```

### Phase 3

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat test --tests "*Migration*" --tests "*Repository*" --tests "*Performance*"
```

### Phase 4

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
.\gradlew.bat test --tests "*Security*" --tests "*Validation*" --tests "*Auth*"
```

### Phase 5

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
npm run test:e2e -- --reporter=line --workers=1
```

### Phase 6

```powershell
.\gradlew.bat clean test
.\gradlew.bat build
docker compose up -d postgres
```

## 5. Definition Of Done For Production-Ready

- No public demo credential, public secret, or mock money/email flow is active in production.
- Production startup fails if required database, payment, email, or external API configuration is absent or placeholder-like.
- PostgreSQL/Flyway schema is verified cleanly without relying on H2 for production validation.
- Payment, email, and refund flows are provider-backed in production and idempotent.
- Hotel data import uses a legal source/API, stores provenance, respects rate limits/terms, and prevents duplicate bulk imports.
- Search/detail/admin flows work with imported real data.
- Security tests cover H2 denial, admin route access, IDOR, CSRF-sensitive routes, and prod mock disablement.
- Build, unit tests, integration tests, migration tests, and browser smoke tests pass.
- README, env example, source decision docs, payment/email/refund docs, deployment checklist, and rotation checklist are complete enough for another engineer to deploy.

## 6. Data Source/API Decision Log

- Initial direction: OpenStreetMap/Overpass is the likely MVP source for legal no-key accommodation place data because it can provide real place names, coordinates, tags, addresses, website/phone when tagged, and clear attribution obligations.
- Google Places can enrich places, ratings, photos, and formatted addresses, but caching/storage terms are stricter and an API key/billing account is required.
- Amadeus Hotel APIs are appropriate for real offers/availability when credentials are available, but they are not a substitute for no-key MVP place import.
- Expedia Rapid or similar partner APIs should only be implemented when partner access and terms are available.
- Internal room/rate templates may be generated only when clearly labeled with `room_source=INTERNAL_TEMPLATE` and `rate_source=INTERNAL_ESTIMATE`; they must never be presented as provider rates.

## 7. Work Blocked Without Real Credentials

- Production payment charge/refund confirmation cannot be completed without a real provider account, webhook secret, sandbox/production keys, and callback/IPN URLs.
- Production SMTP/email delivery cannot be verified without valid SMTP credentials or a transactional email provider key.
- Google Places/Amadeus/Expedia enrichment cannot be run without API keys, billing/partner approval, and terms review.
- Railway/PostgreSQL rotation cannot be completed without access to the Railway project dashboard or owner-provided credentials.
- Production deployment cannot be fully verified without target hosting variables, public HTTPS URL, DNS/certificate setup, and provider webhook configuration.
