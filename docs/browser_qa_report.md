# Browser QA Report

## Status

- Codex in-app Browser: PASS. The in-app browser opened `http://localhost:8081/` and read the live CSS tokens.
- Chrome CDP fallback: PASS. A real local Chrome instance was opened and automated through DevTools Protocol.
- HTTP/API QA fallback: PASS against `http://localhost:8081`.
- Gradle test suite: PASS with `.\gradlew.bat test`.

## Browser Automation Evidence

- Runner: `node scripts/qa/browser_cdp_smoke_test.mjs`
- Base URL: `http://localhost:8081`
- Latest booking created through browser flow: `b424cd37-07c8-419f-801f-6ba4e237b1dc`
- Console/runtime errors after favicon fix: none relevant.
- Mobile overflow checks: PASS for home, search, and login CAPTCHA state.
- In-app browser token check: primary `#2563eb`, accent `#06b6d4`, background `#eef4ff`.
- Runtime computed-color audit for restricted hue band: PASS, `flaggedCount = 0`.

## Browser Flow Checks

- Home loads with navbar, search panel, and CSS tokens.
- Search form submits with check-in/check-out/guest data.
- Search results render room rows.
- Room detail page renders booking form and preserves `roomId`, `checkIn`, `checkOut`, and `guests`.
- Guest booking submit redirects to login.
- Customer login works.
- Customer booking submit redirects to checkout.
- Mock payment starts and success callback confirms booking.
- Booking history renders.
- Customer logout works.
- Admin login works.
- Admin dashboard, rooms, bookings, and users pages render.
- CAPTCHA login state renders.

## Screenshots

- `docs/qa-screenshots/01-home-desktop.png`
- `docs/qa-screenshots/02-search-results-desktop.png`
- `docs/qa-screenshots/03-room-detail-desktop.png`
- `docs/qa-screenshots/04-login-after-guest-booking.png`
- `docs/qa-screenshots/05-checkout.png`
- `docs/qa-screenshots/06-mock-payment.png`
- `docs/qa-screenshots/07-payment-result.png`
- `docs/qa-screenshots/08-booking-history.png`
- `docs/qa-screenshots/09-admin-dashboard.png`
- `docs/qa-screenshots/10-home-mobile.png`
- `docs/qa-screenshots/11-search-mobile.png`
- `docs/qa-screenshots/12-login-captcha-mobile.png`

## HTTP/API Evidence

- GET `/`: 200
- GET `/rooms/search`: 200
- GET `/rooms/{id}`: 200
- GET `/css/app.css`: 200
- GET `/favicon.svg`: 200
- POST `/login` as customer: 200
- POST `/bookings` + POST `/payments/mock/start/{id}` + GET mock success callback: PASS
- POST `/bookings/{id}/cancel`: 200
- POST `/admin/bookings/{id}/check-in`: 200
- POST `/admin/bookings/{id}/check-out`: 200
- POST `/bookings/{id}/review`: 200
- GET `/admin`, `/admin/rooms`, `/admin/users`: 200
- GET `/login?error&captcha`: 200 and CAPTCHA hook visible

## Latest HTTP/API Entities

- Booking A paid, checked in/out, reviewed: `0f9f6e38-13ed-47ed-ba29-213b47ad12ff`
- Booking B paid then cancelled: `c7ac6d51-9a5e-406e-a642-9bb1a9d455dd`

## Notes

- Browser screenshots were taken with Chrome CDP, while token and runtime hue audit were verified in the Codex in-app Browser.
- A missing favicon caused a browser 404 during the first CDP run. Added `src/main/resources/static/favicon.svg`, linked it from templates, and permitted `/favicon.svg` in Spring Security; static favicon now returns 200.
