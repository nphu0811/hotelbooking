# Business Rules

## Search and Availability

- Check-in date must be today or later.
- Check-out date must be after check-in.
- Guest count must be from 1 to 10.
- Stay length must be 30 nights or fewer.
- Search returns only non-deleted `AVAILABLE` rooms with no overlapping `CONFIRMED` or non-expired `PENDING_PAYMENT` booking in `[check_in, check_out)`.
- Default sort is rating descending; filter by price, room type, amenities; paginate 20 rooms/page.

## Booking

- Booking creation requires authenticated user.
- Creation must lock the target room with pessimistic write semantics before checking overlap.
- Price is snapshotted at booking time and never changed by later room price updates.
- `PENDING_PAYMENT` hold expires after 15 minutes.
- Valid booking transitions:
  - `PENDING_PAYMENT -> CONFIRMED` after verified successful payment.
  - `PENDING_PAYMENT -> EXPIRED` after timeout.
  - `PENDING_PAYMENT -> CANCELLED` after failed payment or user cancellation during payment.
  - `CONFIRMED -> CHECKED_IN` by admin.
  - `CONFIRMED -> CANCELLED` by customer/admin cancellation.
  - `CHECKED_IN -> CHECKED_OUT` by admin.
  - `CANCELLED -> REFUNDED` after successful refund where applicable.

## Payment

- Payments use sandbox/mock in dev/test; no real money transaction.
- Every payment has a unique `order_id`.
- IPN/callback handling is idempotent by `order_id`.
- HMAC verification is mandatory for non-mock callback paths.
- Return URL must read payment state from DB and must not trust query parameters.
- Reconciliation runs every 5 minutes for old `INITIATED` payments.
- `INITIATED` payments older than 15 minutes are marked `TIMEOUT` and their booking becomes `EXPIRED`.

## Cancellation and Refund

- Customers can cancel only their own `CONFIRMED` bookings.
- Bookings cannot be cancelled after check-in time without manual handling.
- Refund policy:
  - >= 3 days before check-in: 100%.
  - 1-2 days before check-in: 50%.
  - Check-in day: 0%.
- Refund request uses idempotency key equal to booking id.
- Duplicate cancellation returns the original result, not a new refund.

## Auth and Security

- Passwords use BCrypt with strength at least 12.
- Login error must not reveal whether email exists.
- CAPTCHA hook appears after 3 failed attempts.
- Account locks after 5 failed attempts in 15 minutes.
- Refresh tokens are stored in HttpOnly cookies.
- Users can only access their own bookings, cancellations, and reviews.
- Admin-only pages require role `ADMIN`; role elevation to `ADMIN` requires `SUPER_ADMIN`.
- Locked users must have active JWTs revoked or rejected through blacklist/account-status checks.

## Email

- Event email is asynchronous and must not block main transaction flow.
- Email events: booking confirmed, cancelled, check-in, check-out, review request.
- Retry max is 3 attempts.
- Bounce stops retry and marks email invalid.
- Every send attempt is logged.

## Room and Admin

- Room name length: 2..100 characters.
- Room name cannot duplicate within the same hotel.
- Price must be positive and not exceed 100,000,000 VND.
- Capacity must be 1..10.
- Images: max 10; each <= 5 MB; JPG/PNG/WebP.
- Deleting a room is soft delete and is blocked when active bookings exist.
- Admin changes write audit logs.

## Reviews

- Review is allowed only after `CHECKED_OUT`.
- One booking can have only one review.
- Overall and criterion ratings are from 1 to 5.
- Content length is 50..2000 characters.
- Max review images: 5.
- Profanity filter blocks inappropriate content.
