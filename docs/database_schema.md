# Database Schema

Schema target: PostgreSQL on Railway through Flyway.

## Tables

- `roles`: role catalog with `USER`, `ADMIN`, `SUPER_ADMIN`.
- `users`: accounts, verification, lockout, email validity, UTC timestamps.
- `user_roles`: many-to-many user roles.
- `hotels`: hotel identity, address, geo coordinates, soft delete.
- `rooms`: room catalog, status, price, capacity, rating aggregate, soft delete.
- `room_images`: gallery and primary image.
- `amenities`: reusable amenity catalog.
- `room_amenities`: room amenity join table.
- `bookings`: central booking lifecycle with date range, status, price snapshot, expiry, check-in/out timestamps.
- `payments`: payment order, status, idempotency key, provider payloads.
- `refund_requests`: idempotent refund workflow.
- `reviews`: one review per booking with criterion ratings.
- `review_images`: optional review images.
- `email_jobs`: async email queue.
- `email_logs`: send attempt/result records.
- `audit_logs`: admin/security-sensitive changes.
- `login_logs`: login success/failure tracking.
- `jwt_token_blacklist`: revoked token hashes.

## State Constraints

- User status: `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`.
- Room status: `AVAILABLE`, `MAINTENANCE`.
- Booking status: `PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
- Payment status: `INITIATED`, `SUCCESS`, `FAILED`, `TIMEOUT`, `REFUNDED`.
- Refund status: `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`.

## Data Integrity

- `check_out > check_in`.
- `guest_count` from 1 to 10.
- `nights` from 1 to 30.
- `price_per_night` and snapshots must be positive.
- Review ratings from 1 to 5.
- `reviews.booking_id` is unique: one review per booking.
- Room deletion uses `is_deleted`; business data is not hard-deleted.

## Double Booking Protection

- Service layer must use pessimistic locking while checking/creating bookings.
- Database adds exclusion constraint `ex_bookings_no_overlap` over `(room_id, daterange(check_in, check_out, '[)'))` for active statuses.
- Search and booking use the same interval model: `[check_in, check_out)`.

## Indexing

- Trigram GIN indexes on hotel/room names and hotel address for fuzzy search.
- Room indexes for hotel/status, price, capacity.
- Booking indexes by user, room/date, status, check-in/check-out, and pending expiry.
- Payment index by order id and status/created time.
- Review index by room.
- Email queue index by status and next attempt time.
