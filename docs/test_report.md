# Test Report

## Latest Command

- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `TEST-com.example.demo.HotelBookingApplicationTests.xml` reports `tests="11" skipped="0" failures="0" errors="0"` at `2026-05-23T18:28:27.781Z`.

## Latest Local HTTP QA

- Command: PowerShell `Invoke-WebRequest -UseBasicParsing` flow against `http://localhost:8081`.
- Result: PASS
- Evidence: home/search/detail/customer login/create booking/mock payment/payment result/history/logout/admin dashboard all returned 200 through real local HTTP requests. Booking `88d297f1-4014-45e7-aa33-1e52412e9692` appeared in history after mock payment success.

## Latest Railway-Backed HTTP QA

- Command: Spring Boot `bootRun` started with existing Railway `.env` variables, then PowerShell `Invoke-WebRequest -UseBasicParsing` flow against `http://localhost:8080`.
- Result: PASS
- Evidence: booking `28eeb724-a5c7-4ee6-add2-3b2f59874780` was created through the app and then verified directly in Railway PostgreSQL as booking `CONFIRMED` with payment `SUCCESS`.

## Coverage

- Context load.
- Home page renders.
- Search page renders seeded room data.
- Unauthenticated booking redirects to login.
- Authenticated customer can create a `PENDING_PAYMENT` booking.
- Overlapping booking is rejected by the booking service.
- Mock payment callback is idempotent after a successful callback.
- USER role cannot open `/admin`; ADMIN role can open `/admin`.
- Logout accepts CSRF and redirects home.
- Nav hides login/register from authenticated users, hides history/admin from anonymous users, and hides admin from customers.
- Failed login attempts show a CAPTCHA hook after 3 failures and lock the account after 5 failures in a 15-minute window.
- Jackson serializes `Instant` values as UTC text with `Z`.
- HTTP QA previously verified local search/detail/login/booking/mock payment/history against `localhost:8080`.
- HTTP QA verified the latest local code against `localhost:8081`, including visible CAPTCHA hook and booking `88d297f1-4014-45e7-aa33-1e52412e9692`.
- Railway-backed HTTP QA verified app read/write against the real Railway PostgreSQL database.

## Gaps

- Browser E2E pending.
- Full JWT API tests are not present because the current implementation uses Spring Security form login/session for the UI.
