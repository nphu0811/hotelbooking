# Frontend Discovery

## Stack

- Frontend rendering: Spring MVC + Thymeleaf server-rendered templates.
- Styling: plain CSS from `src/main/resources/static/css/app.css`.
- Template security helpers: Thymeleaf Spring Security dialect via `sec:authorize`.
- JavaScript: none in the current frontend.
- Assets: local SVG room placeholders in `src/main/resources/static/css/`.
- Build/runtime: Spring Boot 4.0.6, Gradle, local H2 profile, PostgreSQL/Railway production profile.

## Main Files

- Shared navigation: `src/main/resources/templates/fragments/nav.html`.
- Global CSS entry: `src/main/resources/static/css/app.css`.
- Home: `src/main/resources/templates/home.html`.
- Search/list: `src/main/resources/templates/rooms/search.html`.
- Room detail: `src/main/resources/templates/rooms/detail.html`.
- Auth: `src/main/resources/templates/auth/login.html`, `src/main/resources/templates/auth/register.html`.
- Booking/payment: `src/main/resources/templates/bookings/*.html`.
- Admin: `src/main/resources/templates/admin/*.html`.
- Error page: `src/main/resources/templates/error.html`.

## Pages

- Home landing with search form and featured room cards.
- Room search with filters, sort select, room result rows, empty state.
- Room detail with image, amenities, policy, reviews, and booking form.
- Login/register forms with CSRF tokens and validation messages.
- Checkout, mock payment, and payment result screens.
- Customer booking history and booking detail/review/cancel screens.
- Admin dashboard, room management, booking management, user management.
- Generic business error screen.

## Shared Components

- `topbar`, `brand`, `nav-links`, `nav-auth`.
- `button`, `button-muted`, `button-danger`, `button-small`.
- `search-panel`, `toolbar`, `auth-panel`, `booking-box`, `summary`.
- `room-grid`, `room-card`, `room-list`, `room-row`.
- `chips`, `badge`, `alert`, `success`, `empty`, `muted`.
- `table-wrap`, `inline-form`, `admin-grid`, `admin-tile`.

## Binding And Route Constraints

- Keep all `th:href`, `th:action`, `th:text`, `th:value`, `th:if`, `th:each`, and `sec:authorize` bindings.
- Keep CSRF hidden inputs in all POST forms.
- Do not rename form input names:
  - Search: `q`, `checkIn`, `checkOut`, `guests`, `minPrice`, `maxPrice`, `sort`.
  - Booking: `roomId`, `checkIn`, `checkOut`, `guests`, `specialRequest`.
  - Auth: `username`, `password`, `fullName`, `email`, `phone`, `confirmPassword`.
  - Admin room update: `price`, `status`.
  - User lock: `reason`.
  - Review: `rating`, `cleanlinessRating`, `serviceRating`, `locationRating`, `valueRating`, `content`.
- Keep important routes:
  - `/`, `/rooms/search`, `/rooms/{id}`, `/bookings`, `/checkout/{id}`.
  - `/payments/mock/start/{id}`, `/payments/mock/{orderId}`, `/payments/mock/callback`.
  - `/account/bookings`, `/account/bookings/{id}`.
  - `/admin`, `/admin/rooms`, `/admin/bookings`, `/admin/users`.
  - `/login`, `/register`, `/logout`, `/verify/{token}`.

## Redesign Risks

- Navigation must preserve role-based visibility for anonymous, user, admin, and super admin states.
- POST forms must keep CSRF tokens; logout must remain a POST form.
- Payment mock links include signed query params and must remain intact.
- Admin inline forms are table-based and need mobile-safe overflow rather than broken stacking.
- Room images are SVG placeholders, so the redesign should make them feel intentional.
- Browser automation is currently externally blocked in this session; verification needs build/test and HTTP fallback unless Browser becomes available.
