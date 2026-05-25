# Phase 0 Verification Report

Date: 2026-05-25

## Scope

Security containment only. No hotel API/data integration was added.

## Changes Verified

- Login UI no longer pre-fills or displays public demo credentials.
- `DataSeeder` no longer creates customer/admin accounts.
- `DataSeeder` only runs when both conditions are true:
  - active profile is `local`, `dev`, or `test`
  - `app.seed-demo-data=true`
- `application-prod.properties` requires environment-provided PostgreSQL, payment, and email configuration.
- Production validator rejects H2 datasource URLs, missing values, placeholder-like values, mock payment, enabled H2 console, and demo seeding.
- `/h2-console/**` and `/payments/mock/**` are denied outside local/dev/test by an early servlet filter and the security chain.
- Production security headers are configured: CSP, HSTS, Referrer-Policy, Permissions-Policy, X-Content-Type-Options, and DENY frame options outside H2-local mode.
- Session cookie flags are set in production profile: Secure, HttpOnly, SameSite=Strict.
- Mock payment callback is POST-only in local/dev/test UI and no longer exposes a query-string signing secret.
- Non-console email providers fail closed instead of marking jobs `SENT` without an implementation.

## Verification Commands

```powershell
rg -n "customer@example\.test|admin@example\.test|User@123|Admin@123|mock-secret|Demo customer|Demo admin|tài khoản demo|tai khoan demo" src/main
.\gradlew.bat test --tests "*ProductionSecurityTests"
.\gradlew.bat clean test
.\gradlew.bat build
```

## Results

- `src/main` credential/mock-secret scan: PASS, no matches.
- `ProductionSecurityTests`: PASS.
- `.\gradlew.bat clean test`: PASS, 16 tests.
- `.\gradlew.bat build`: PASS.

## Remaining Phase 1 Blockers

- Payment still needs a real provider abstraction, HMAC webhook verification, raw webhook storage, idempotency, and reconciliation.
- Email still needs SMTP provider implementation, provider message ID storage, retry/backoff, and token-safe logging.
- Refund still needs provider-backed processing and reconciliation before money movement can be considered real.
