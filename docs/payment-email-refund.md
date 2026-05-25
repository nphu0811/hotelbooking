# Payment, Email, Refund Architecture

## Payment

Production payment no longer depends on a public GET callback or a hard-coded mock secret.

### Interfaces

- `PaymentProvider`
  - `createPaymentIntent(...)`
  - `verifyWebhook(...)`
  - `parseWebhook(...)`
  - `refund(...)`
  - `getProviderName()`
- `PaymentProviderRegistry` selects providers by `app.payment.provider`.

### Providers

- `MockPaymentProvider`
  - Active only in `local`, `dev`, and `test`.
  - Used by local browser and unit tests.
  - Disabled and denied in production.
- `VnPayPaymentProvider`
  - Production adapter skeleton.
  - Creates signed VNPay payment URLs.
  - Verifies VNPay HMAC SHA-512 webhook/IPN payloads.
  - Parses amount, currency, order ID, booking ID, provider transaction ID, and status.

### Webhook/IPN

- Endpoint: `POST /payments/{provider}/webhook`
- The webhook endpoint is public and CSRF-exempt because payment providers call it server-to-server; every state change still requires provider signature validation.
- Raw payload is stored in `payment_webhook_events`.
- Event idempotency uses `provider_event_id`.
- Payment updates are idempotent; already-paid/refunded payments are not confirmed twice.
- Webhook validation checks:
  - provider signature
  - amount
  - currency
  - booking ID
  - order ID
- provider transaction ID when present

### Return Page

- Endpoint: `GET /payments/{provider}/return`
- The return page is read-only. It looks up the stored payment status and does not confirm payment without a valid provider webhook/IPN.

### Statuses

- `PENDING`
- `PAID`
- `FAILED`
- `CANCELLED`
- `REFUND_PENDING`
- `REFUNDED`

## Email

Email is provider-backed before a job is marked `SENT`.

### Interfaces

- `EmailProvider`
  - `send(...)`
  - `getProviderName()`
- `EmailProviderRegistry` selects providers by `app.email.provider`.

### Providers

- `ConsoleEmailProvider`
  - Active only in `local`, `dev`, and `test`.
  - Marks local/test messages sent with a `CONSOLE-*` provider message id.
- `SmtpEmailProvider`
  - Sends through SMTP using env-provided `MAIL_*` values.
  - Sends account verification emails with a one-time `/verify/{token}` link built from `APP_PUBLIC_BASE_URL`.
  - Records a provider message id on success.
  - Sanitizes errors so the SMTP password is not logged.

### Retry

- Jobs start as `PENDING`.
- Failed send attempts become `RETRYING` until max attempts.
- Max attempts: 3.
- Backoff: exponential minutes based on attempt count.
- Final failure state: `FAILED`.

## Refund

Cancellation no longer marks money movement as complete unless the provider says the refund is complete.

### Flow

1. Customer requests cancellation.
2. `BookingService` validates booking ownership, status, and cancellation policy.
3. A `RefundRequest` is created with an idempotency key.
4. `PaymentService.submitRefund(...)` sends refund request to the payment provider.
5. If provider completes immediately, refund becomes `SUCCEEDED`, payment becomes `REFUNDED`, and booking becomes `REFUNDED`.
6. If provider accepts but does not complete immediately, refund remains `PROCESSING` and payment becomes `REFUND_PENDING`.
7. Provider webhook/reconciliation must later complete the refund.

## Production Behavior

- `app.payment.mock.enabled=false`
- `/payments/mock/**` is denied outside local/dev/test.
- `app.email.provider=smtp`
- Production startup fails if required payment/email secrets are missing or placeholder-like.

## Current Limitations

- VNPay refund settlement still needs final provider-specific reconciliation/webhook mapping.
- MoMo/Expedia/other payment providers are not implemented.
- Real SMTP delivery requires valid credentials and a reachable provider.
