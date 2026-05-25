# Final Production Readiness Report

Date: 2026-05-25

## Result

The application code is now deployable as a production-profile build artifact, with no remaining known critical/high code blockers from the previous FAANG-level review.

True production go-live still requires live external-provider verification with owner-controlled credentials and a public HTTPS staging URL. Do not claim real payment/email production completion until those checks pass.

## Fixed In This Pass

- VNPay webhook/IPN endpoint is public and CSRF-exempt while still requiring provider HMAC validation before state changes.
- VNPay return endpoint is read-only and does not confirm payment without webhook/IPN state.
- VNPay payment URL now includes provider-style create time and client IP.
- Verification emails now contain a real `/verify/{token}` link built from `APP_PUBLIC_BASE_URL`; only the token hash is stored.
- SMTP sends actual rendered message body instead of only the template name.
- Production profile now requires public HTTPS URLs for `APP_PUBLIC_BASE_URL`, `VNPAY_RETURN_URL`, and `VNPAY_IPN_URL`.
- Production config no longer accepts MoMo until a real adapter exists.
- Removed the real-looking Geoapify key from test fixtures and strengthened sanitizer coverage for free-text key leaks.
- E2E fixtures are unique per browser project, preventing desktop/mobile date conflicts.
- Removed infinite badge animation that caused unstable browser clicks.

## Verification Evidence

- `.\gradlew.bat clean test`: PASS
- `.\gradlew.bat build`: PASS
- `npm run test:e2e -- --reporter=line --workers=1`: PASS, 4/4
- Geoapify local dry-run: PASS, fetched 5, inserted 5, updated 0, skipped 0
- Secret scan for known high-risk tokens/real Geoapify key: PASS, no matches
- VNPay HTTP webhook regression: PASS, anonymous signed IPN reaches controller and confirms payment

## Screenshots

- `docs/qa-screenshots/e2e-payment-chromium.png`
- `docs/qa-screenshots/e2e-admin-chromium.png`
- `docs/qa-screenshots/e2e-payment-mobile-chrome.png`
- `docs/qa-screenshots/e2e-admin-mobile-chrome.png`

## Remaining External Go-Live Gates

- Deploy to a public HTTPS staging URL.
- Configure VNPay sandbox credentials.
- Set `APP_PUBLIC_BASE_URL`, `VNPAY_RETURN_URL`, and `VNPAY_IPN_URL` to that public URL.
- Run real VNPay sandbox payment.
- Verify real VNPay IPN delivery.
- Test duplicate IPN.
- Test wrong signature.
- Configure real SMTP credentials.
- Send and open a real account verification email.
- Send a real booking confirmation email.
- Run secret scan on repo, logs, and private environment files.
- Rotate any credential ever pasted into local files, logs, chats, or screenshots.
- Run clean PostgreSQL/Flyway migration verification with Docker/Testcontainers or another disposable PostgreSQL database.

## Readiness

- Portfolio-ready: Yes.
- MVP/staging-ready: Yes, for code and local browser verification.
- Production deployable: Yes, when all required production environment variables are provided.
- Production go-live complete: Not until the external gates above pass.

## Final Score

- Code/staging readiness: 96/100.
- Real production go-live readiness before external provider verification: 88/100.
