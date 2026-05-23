# Workflows

## Guest Search to Room Detail

1. Guest opens home page.
2. Guest enters location/name, check-in, check-out, guest count.
3. System validates input and queries available rooms using overlap exclusion.
4. Guest filters/sorts result list.
5. Guest opens a room detail page.
6. System shows gallery, amenities, availability calendar, map placeholder, cancellation policy, and reviews.

## Registration and Verification

1. Customer submits registration form.
2. System validates email, phone, full name, and password strength.
3. System creates `PENDING_VERIFICATION` user and email verification token.
4. System queues verification email.
5. Customer opens verification link.
6. System activates the account and allows login.

## Booking and Mock Payment

1. Customer chooses dates and guests from room detail.
2. System requires login, then starts a transaction.
3. System locks room row and checks overlap.
4. System creates `PENDING_PAYMENT` booking with 15-minute expiry and price snapshot.
5. Customer confirms payment method.
6. System creates payment order and routes to mock/sandbox payment.
7. IPN/callback verifies signature/idempotency and marks payment success/failure.
8. Success confirms booking and queues confirmation email.

## Booking History and Cancellation

1. Customer opens booking history.
2. System queries bookings by authenticated user id.
3. Customer selects a confirmed booking and requests cancellation.
4. System validates ownership and state.
5. System calculates refund percentage and amount.
6. Customer confirms.
7. System cancels booking, creates idempotent refund request, and queues cancellation email.

## Admin Booking Operations

1. Admin opens booking management.
2. System lists and filters bookings.
3. Admin opens detail.
4. Admin check-ins a `CONFIRMED` booking around check-in date.
5. System writes actual check-in timestamp, audit log, and email event.
6. Admin check-outs a `CHECKED_IN` booking.
7. System writes check-out timestamp, audit log, and review request email event.

## Email Queue

1. Business module creates an email job in the same transaction or immediately after successful state change.
2. Worker selects pending jobs.
3. Worker renders the correct template.
4. Worker sends through configured provider or mock sender.
5. Worker logs `SENT`, `FAILED`, `BOUNCED`, or `COMPLAINED`.
6. Failed retryable jobs are retried up to 3 attempts.

## Payment Timeout and Reconciliation

1. Scheduler runs every minute for booking expiry and every 5 minutes for reconciliation.
2. Expiry job marks stale `PENDING_PAYMENT` bookings `EXPIRED`.
3. Reconciliation job checks `INITIATED` payments older than 10 minutes.
4. Mock provider returns safe deterministic status in local/test.
5. System updates payment and booking state idempotently.
