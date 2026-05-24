# Implementation Checklist

## Phase A - Requirements

- [x] Read `hotel_booking_markdown_package/hotel_booking_report.md`.
- [x] Extract actors.
- [x] Map UC01-UC12.
- [x] Extract business rules.
- [x] Extract workflows.
- [x] Draft acceptance criteria.
- [x] Verify document coverage with command output.

## Phase B - Database

- [x] Create Flyway V1 schema.
- [x] Create Flyway V2 indexes/constraints.
- [x] Document schema and ERD.
- [x] Add Railway check SQL.
- [x] Add local reset SQL.
- [x] Run local H2 schema/test migration fallback.
- [x] Run Railway migration if env credentials are available.

## Phase C - UTC

- [x] Configure Hibernate JDBC timezone UTC in `application.properties`.
- [x] Configure Jackson timezone UTC in `application.properties`.
- [x] Verify live PostgreSQL/Railway DB timezone with accepted existing credential.
- [x] Verify local app/log timestamp rendering.
- [x] Verify app timestamp serialization.
- [x] Document current results.

## Phase D - Railway

- [x] Use Railway credential from environment only.
- [x] Verify connection without printing secret.
- [x] Verify schema.
- [x] Verify app read/write through HTTP QA fallback.
- [x] Document verification.

## Phase E - Dataset

- [x] Select legal/free/open dataset or synthetic fallback.
- [x] Create raw/processed folders.
- [x] Add download/transform/import scripts.
- [x] Import seed data through local `DataSeeder`.
- [x] Verify table counts through tests.

## Phase F - API Keys

- [x] Add `.env.example`.
- [x] Add `application.properties.example`.
- [x] Document map/payment/email/image/CAPTCHA fallback services.

## Phase G - UI Design

- [x] Create Stitch project.
- [ ] Create design system in Stitch asset. Blocked by Stitch MCP `Request contains an invalid argument`; local design system fallback is implemented.
- [x] Document design tokens.
- [x] Implement Thymeleaf templates and static assets.

## Phase H-J - Application

- [x] Auth/User module.
- [x] Room/Search module.
- [x] Room detail module.
- [x] Booking module.
- [x] Payment module.
- [x] Customer booking history.
- [x] Cancel/refund module.
- [x] Email module.
- [x] Review module.
- [x] Admin module.

## Phase K - QA

- [x] Unit tests.
- [x] Integration tests.
- [x] Security/IDOR tests.
- [ ] Browser QA. Blocked by Browser MCP `Browser is not available: iab`; HTTP fallback passed.
- [x] HTTP local QA fallback.
- [x] Railway QA if credentials are available.
- [x] Logout CSRF and role-scoped nav verified.
- [x] Login failure CAPTCHA hook and temporary account lockout verified.
