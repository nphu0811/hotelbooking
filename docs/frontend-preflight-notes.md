# Frontend Preflight Notes

## Project Structure
- Spring Boot project with Gradle wrapper.
- Main templates: `src/main/resources/templates`.
- Static assets: `src/main/resources/static/css`, `src/main/resources/static/js`, `src/main/resources/static/favicon.svg`.
- Redesign reference folder: `stitch_hotelbooking_premium_front_end_redesign`.

## Frontend Stack
- Thymeleaf server-rendered HTML.
- Plain CSS and vanilla JavaScript.
- Spring Security form login and OAuth2 links.
- No local UI verification used for final QA; local build only.

## Templates Found
- Home: `home.html`.
- Shared fragments: `fragments/nav.html`, `fragments/layout.html`.
- Auth: `auth/login.html`, `auth/login-password.html`, `auth/login-otp.html`, `auth/register.html`, `auth/verification.html`.
- Rooms: `rooms/search.html`, `rooms/detail.html`.
- Bookings/payment: `bookings/checkout.html`, `bookings/detail.html`, `bookings/history.html`, `bookings/mock-payment.html`, `bookings/payment-result.html`.
- User: `profile.html`, `recommend.html`.
- Admin: `admin/dashboard.html`, `admin/rooms.html`, `admin/bookings.html`, `admin/users.html`.
- Error: `error.html`.

## Static Assets Found
- CSS before refactor: `tokens.css`, `base.css`, `layout.css`, `components.css`, `pages.css`, `animations.css`, `theme.css`, `responsive.css`, `luxury.css`.
- Active CSS after refactor: `app.css` importing `lumiere.css`.
- JS: `theme.js`, `nav.js`, `home-ai.js`, `booking-hold.js`, `auth-validation.js`.
- Favicon replaced with L/È platinum monogram.

## Auth Routes Found
- Password login page: `/login/password`, posts to `/login` using `username` and `password`.
- OTP request page: `/login`, `/login/otp`, `/login-otp`, posts to `/login/otp/request` using `identifier`.
- OTP verify page: server renders `auth/login-otp.html`, posts to `/login/otp/verify` using `identifier` and `otp`.
- Register page: `/register`, `/signup`.
- Verification page: `/verification`.

## Header Navigation Behavior
- Existing deployed header used same-page hash scrolling for sections.
- Redesign keeps hash navigation and smooth scroll with `nav.js`.
- Section targets used: `#rooms`, `#hanoi`, `#ai-recommendation`, `#offers`.
- Auth/admin/profile/book-now links remain route links.

## Railway Deployment Setup
- Remote: `https://github.com/nphu0811/hotelbooking.git`.
- Branch at preflight: `main`.
- Deployment target: `https://hotelbooking-production-57a9.up.railway.app`.
- Dockerfile and Gradle build are present.

## Risk Areas
- Thymeleaf expressions in shared nav and home search forms.
- Spring Security form field names and route separation.
- CSP blocks external scripts/fonts, so redesign uses local CSS/JS only.
- Final QA must be on Railway deployed URL, not local.
