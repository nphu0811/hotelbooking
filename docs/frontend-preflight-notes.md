# Frontend Preflight Notes

## Project Structure
- Spring Boot application under `src/main/java/com/example/demo`.
- Thymeleaf templates under `src/main/resources/templates`.
- Static CSS/JS/assets under `src/main/resources/static`.
- Database migrations under `src/main/resources/db/migration`.
- Gradle build with wrapper: `gradlew.bat build`.

## Frontend Stack
- Server-rendered Thymeleaf.
- CSS bundle entry: `src/main/resources/static/css/app.css`.
- JS modules: `theme.js`, `nav.js`, `auth-validation.js`, `home-ai.js`, `booking-hold.js`.
- Spring Security and Thymeleaf Spring Security extras are used in shared nav.

## Templates Found
- Home: `home.html`.
- Shared fragments: `fragments/layout.html`, `fragments/nav.html`.
- Auth: `auth/login.html`, `auth/login-password.html`, `auth/login-otp.html`, `auth/register.html`, `auth/verification.html`.
- Rooms: `rooms/search.html`, `rooms/detail.html`.
- Bookings/payment: `bookings/checkout.html`, `bookings/detail.html`, `bookings/history.html`, `bookings/payment-result.html`, `bookings/mock-payment.html`.
- User: `profile.html`, `recommend.html`.
- Admin: `admin/dashboard.html`, `admin/rooms.html`, `admin/bookings.html`, `admin/users.html`.
- Error: `error.html`.

## Static Assets Found
- CSS: tokens, base, layout, components, pages, animations, theme, responsive.
- SVG placeholders: room placeholder/city/room SVG files and `favicon.svg`.
- JS: theme toggle, nav/mobile menu, auth validation, AI recommendation, booking hold countdown.

## Auth Routes Found
- `GET /login` renders OTP login entry.
- `GET /login/password` renders password login.
- `POST /login` is Spring Security password processing.
- `POST /login/otp/request` requests OTP.
- `POST /login/otp/verify` verifies OTP.
- `GET /register` and `POST /register` handle registration.
- `GET/POST /verification` handle account verification.
- Preflight issue found: deployed `/login/otp` returned 404, so a GET route must be added.

## Header Navigation Behavior
- Shared nav is in `fragments/nav.html`.
- Public nav items are expected to preserve single-page scroll behavior for home sections.
- Login/register/profile/admin/logout remain route-based actions.
- Preflight issue found: deployed mobile nav displayed full links instead of a compact menu.

## Railway Deployment Setup
- Dockerfile builds with Gradle and runs the generated Spring Boot jar.
- Production profile is set through `SPRING_PROFILES_ACTIVE=prod`.
- Git remote: `https://github.com/nphu0811/hotelbooking.git`.
- Current branch during preflight: `main`.

## Risk Areas
- Existing worktree had unrelated backend/config changes before this redesign pass; they were not reverted.
- `application*.properties` are modified locally and may contain deploy-sensitive configuration, so they must not be staged blindly.
- Auth form action/name fields must remain aligned with Spring Security and `AuthController`.
- Header anchors must work on Railway, not only locally.
- CSP blocks inline scripts, so UI behavior must live in external JS.
