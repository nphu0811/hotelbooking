# Acceptance Criteria

## UC01

- Search rejects invalid dates, guest counts outside 1..10, and stays over 30 nights.
- Search excludes rooms with overlapping confirmed or pending-payment bookings.
- Result page supports filters, sorting, pagination, and empty state.

## UC02

- Room detail renders gallery, amenities, policy, calendar, and reviews.
- Soft-deleted rooms return 404.
- Maintenance rooms cannot start booking.

## UC03

- Register creates `PENDING_VERIFICATION` user and queues email.
- Verification activates account.
- Login uses BCrypt and generic errors.
- Failed login lockout and CAPTCHA hook are enforced or visibly stubbed for dev.

## UC04

- Booking requires authentication.
- Booking creation uses transaction and pessimistic lock.
- Double booking is rejected.
- Pending booking expires after 15 minutes.
- Successful payment confirms booking and queues email.

## UC05

- Payment order id is unique.
- Mock payment success/failure works locally.
- Duplicate callback is idempotent.
- Invalid HMAC is rejected for signed callback path.
- Return page reads DB state.

## UC06

- Customer sees only own bookings.
- Booking history supports status/date filters and empty state.
- Direct access to another user's booking is blocked.

## UC07

- Only own confirmed booking can be cancelled.
- Refund amount follows 100/50/0 policy.
- Duplicate cancel does not create duplicate refund.
- Cancellation queues email.

## UC08

- Admin can create/edit/status-change rooms with validation.
- Soft delete is blocked by active bookings.
- Room changes write audit logs.

## UC09

- Admin can check-in only valid confirmed bookings.
- Admin can check-out only checked-in bookings.
- Invalid transitions are rejected and logged.

## UC10

- Email jobs are created for required events.
- Retry stops after 3 failures.
- Send attempts are visible in email logs.

## UC11

- Only checked-out bookings owned by current user can be reviewed.
- Only one review per booking is accepted.
- Ratings and content length are validated.
- Room aggregate rating updates after review.

## UC12

- Admin can list, search, lock, and unlock users.
- SUPER_ADMIN cannot be locked by normal admin.
- Only SUPER_ADMIN can grant ADMIN role.
- Locking user prevents future login and invalidates active tokens where implemented.
