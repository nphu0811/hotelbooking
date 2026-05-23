# HotelBooking Tasks

## Phase A - Requirements Extraction

- Status: COMPLETED
- Source: `hotel_booking_markdown_package/hotel_booking_report.md`
- Required outputs:
  - [x] `docs/requirements_extracted.md`
  - [x] `docs/usecase_matrix.md`
  - [x] `docs/business_rules.md`
  - [x] `docs/workflows.md`
  - [x] `docs/acceptance_criteria.md`
  - [x] `docs/implementation_checklist.md`
- Verification:
  - [x] Confirm UC01-UC12 are represented with `rg`.

## Phase B - PostgreSQL Schema

- Status: IN_PROGRESS
- Required outputs:
  - [x] `src/main/resources/db/migration/V1__init_schema.sql`
  - [x] `src/main/resources/db/migration/V2__indexes_constraints.sql`
  - [x] `docs/database_schema.md`
  - [x] `docs/erd.md`
  - [x] `scripts/db/check_railway_db.sql`
  - [x] `scripts/db/reset_local_db.sql`
  - [x] `docs/db_verification_report.md`

## Phase C - UTC Configuration

- Status: IN_PROGRESS
- Notes: `application.properties` now contains Hibernate/Jackson UTC settings.
- Required output:
  - [ ] `docs/timezone_utc_verification.md`

## Phase D - Railway Verification

- Status: BLOCKED
- Blocker: Real Railway credential must come from environment variables and the previously exposed credential should be rotated before trusted verification.
- Required output:
  - [ ] `docs/railway_verification_log.md`

## Phase E - Dataset Import

- Status: IN_PROGRESS
- Required outputs:
  - [x] `data/raw/`
  - [x] `data/processed/`
  - [x] `scripts/data/download_dataset.*`
  - [x] `scripts/data/transform_hotels.*`
  - [x] `scripts/data/import_to_postgres.*`
  - [x] `scripts/data/verify_import.sql`
  - [x] `docs/dataset_import_report.md`

## Phase F - External Services and API Key Setup

- Status: COMPLETED
- Required outputs:
  - [x] `.env.example`
  - [x] `src/main/resources/application.properties.example`
  - [x] `docs/API_KEYS_SETUP.md`
  - [x] `docs/external_services.md`

## Phase G - UI Design

- Status: IN_PROGRESS
- Required outputs:
  - [x] Stitch project/design system notes
  - [x] `docs/design_system.md`
  - [x] `docs/stitch_design_notes.md`

## Phase H-J - Backend and Frontend Implementation

- Status: IN_PROGRESS
- Modules:
  - [x] Auth/User
  - [x] Room/Search
  - [x] Room detail
  - [x] Booking
  - [x] Payment mock/sandbox
  - [x] Booking history
  - [x] Cancel/refund
  - [x] Email queue/logs
  - [x] Review
  - [x] Admin

## Phase K - Tests and QA

- Status: IN_PROGRESS
- Required outputs:
  - [x] `docs/test_report.md`
  - [x] `docs/browser_qa_report.md`
  - [ ] no severe OPEN entries in `ERROR_LOG.md`
