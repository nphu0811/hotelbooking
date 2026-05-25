# SMTP Setup And Verification

This project sends production email only through `SmtpEmailProvider`.
Do not commit SMTP credentials. Configure them through environment variables or the private `.env` used by the runtime host.

## Required Environment Variables

```properties
APP_EMAIL_PROVIDER=smtp
APP_PUBLIC_BASE_URL=https://staging.example.com
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=mailer@example.com
MAIL_PASSWORD=<smtp-app-password-or-secret>
MAIL_FROM=noreply@example.com
```

Production profile fails fast when `APP_EMAIL_PROVIDER` is not `smtp`, when mail values are missing, or when values look like placeholders.

## Verification Procedure

Use a non-production mailbox and a test recipient owned by the team.

1. Set the required variables in the shell, IDE run configuration, Railway variables, or private `.env`.
2. Start the app with `SPRING_PROFILES_ACTIVE=prod`.
3. Register a new account with the test recipient and verify that an account verification email is received.
4. Open the `/verify/{token}` link from the email and confirm the account becomes active.
5. Complete a booking/payment in the selected payment test flow and verify the booking confirmation email is received.
6. Cancel the booking and verify the cancellation/refund email is received.
7. Temporarily set an invalid SMTP password in a private environment and process pending jobs. The job must move to `RETRYING` or `FAILED`, not `SENT`.

Expected database evidence:

- `email_jobs.status` is `SENT` only after the provider reports success.
- `email_jobs.provider_message_id` and `email_logs.provider_message_id` are populated for successful sends.
- Failure rows contain sanitized error messages and no password/token values.

## Current Verification Status

As of this repository verification pass, real SMTP delivery was not executed because no explicit SMTP test recipient and confirmed non-placeholder SMTP secret were available in the workspace.
Local/test coverage verifies queue processing and provider message id persistence with `ConsoleEmailProvider`; production startup validation verifies that console email is not accepted in `prod`.
