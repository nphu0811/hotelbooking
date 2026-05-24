# Frontend Changelog

## Scope

Premium redesign for the Spring Boot + Thymeleaf hotel booking frontend. The work keeps server routes, Thymeleaf bindings, form actions, input names, CSRF fields, authorization conditions, loops, and CRUD/payment/search flows intact.

## Files Created

- `docs/frontend-discovery.md`
- `docs/frontend-skill-research.md`
- `docs/frontend-visual-direction.md`
- `docs/frontend-audit.md`
- `docs/frontend-design-system.md`
- `docs/frontend-changelog.md`
- `src/main/resources/static/css/tokens.css`
- `src/main/resources/static/css/base.css`
- `src/main/resources/static/css/layout.css`
- `src/main/resources/static/css/components.css`
- `src/main/resources/static/css/pages.css`
- `src/main/resources/static/css/animations.css`
- `src/main/resources/static/css/responsive.css`
- `src/main/resources/static/favicon.svg`
- `scripts/qa/browser_cdp_smoke_test.mjs`

## Files Modified

- `src/main/resources/static/css/app.css`
- `src/main/resources/templates/fragments/nav.html`
- `src/main/resources/templates/home.html`
- `src/main/resources/templates/rooms/search.html`
- `src/main/resources/templates/rooms/detail.html`
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/auth/register.html`
- `src/main/resources/templates/bookings/checkout.html`
- `src/main/resources/templates/bookings/mock-payment.html`
- `src/main/resources/templates/bookings/payment-result.html`
- `src/main/resources/templates/bookings/history.html`
- `src/main/resources/templates/bookings/detail.html`
- `src/main/resources/templates/admin/dashboard.html`
- `src/main/resources/templates/admin/rooms.html`
- `src/main/resources/templates/admin/bookings.html`
- `src/main/resources/templates/admin/users.html`
- `src/main/resources/templates/error.html`
- `src/main/java/com/example/demo/config/SecurityConfig.java`

## Pages Redesigned

- Home / landing: cinematic booking hero, premium search panel, destination/room highlights, service section.
- Auth: elevated login/register panels, better alert states, CAPTCHA hook styling, stronger focus and CTA treatment.
- Room search: refined page header, filter toolbar, modern result cards, empty-state treatment.
- Room detail: large visual gallery, sticky booking summary, amenity and policy sections.
- Booking checkout, payment mock, payment result: clearer payment hierarchy and action panels.
- Customer booking history/detail: status badges, table treatment, review/cancel action surfaces.
- Admin dashboard, rooms, bookings, users: restrained productivity UI with stats, tables, badges, and action controls.
- Error page: branded recovery screen with clear CTA.
- Static icon: added SVG favicon to avoid browser 404 noise.

## Components Refactored

- Navbar: sticky glass header, active/hover treatment, role-aware links preserved.
- Buttons: primary/secondary/danger/ghost states, lift/press/focus interactions.
- Forms: premium inputs, focus rings, validation/alert/success visual language.
- Cards: elevated surfaces, image zoom, subtle border glow and hover lift.
- Tables: responsive wrapper, row hover, readable admin/customer density.
- Badges/alerts: status-aware color tokens and accessible contrast.
- Pagination, empty states, summaries, admin forms, footer.

## Design Tokens Added

- Color system: primary blue, secondary indigo, cyan accent, cool mist background, elevated surfaces, semantic status colors.
- Typography: system UI stack, heading/body/button scales, line heights.
- Layout: container widths, grid columns, dashboard structure, section spacing, header height, breakpoints.
- Radius, shadow, glow, spacing, motion duration and easing variables.

## Animation And Micro-Interactions

- Page enter fade/slide.
- Section/card reveal motion using transform and opacity.
- Button hover lift, press state, shine treatment, focus rings.
- Card hover lift, image zoom, border glow.
- Navbar hover pill/underline behavior.
- Form focus animation.
- Table row hover and action hover.
- Decorative CSS-only hero depth animation.

## Visual Depth / Decoration

- CSS-only ambient gradients, layered background texture, glass surfaces, hero depth panel, and elevated shadows.
- No heavy 3D library added; visual depth is implemented with CSS transforms, gradients, blur, and controlled shadows.

## Responsive Work

- Responsive rules for desktop, laptop, tablet, mobile, and narrow mobile.
- Search/filter forms collapse cleanly.
- Cards and admin stats reflow to single-column layouts.
- Tables use styled horizontal overflow where needed.
- Nav/action groups wrap without blocking content.
- Buttons and forms become touch-friendly on small screens.

## Accessibility Work

- Visible `:focus-visible` ring across interactive controls.
- Form labels and existing input names preserved.
- Alert/success/error states use text plus color.
- Contrast-oriented palette for text, surfaces, badges, and buttons.
- Added `prefers-reduced-motion` fallback to disable motion-heavy effects.
- Keyboard-accessible native links, buttons, forms, selects, and inputs preserved.

## Performance Work

- No new JS or frontend runtime dependency.
- No GSAP, Three.js, Lenis, AOS, or icon package added.
- Animation uses transform/opacity where possible.
- CSS split into explicit architecture while keeping the single existing `app.css` entrypoint.
- Static CSS assets verified via HTTP 200.
- Favicon asset added and verified via HTTP 200.
- `/favicon.svg` is publicly permitted so anonymous pages can load the icon without login redirect.

## Functionality Tested

- `.\gradlew.bat test`: PASS.
- Local app on `http://localhost:8081/`: started with `SPRING_PROFILES_ACTIVE=local`.
- HTTP QA: PASS for home, room search, room detail, guest booking redirect, customer login, booking creation, checkout, mock payment, payment success callback, booking history, logout, admin dashboard, CAPTCHA hook, and CSS assets.
- Chrome CDP browser QA: PASS for desktop flow, mobile overflow checks, screenshots, customer/admin sessions, booking/payment/history/admin pages, and no relevant console/runtime errors.
- Extended HTTP/API QA: PASS for cancel, admin check-in/check-out, review creation, admin pages, CAPTCHA state, CSS, and favicon.
- Restricted-hue palette audit: updated tokens, gradients, badges, buttons, glows, room SVGs, and favicon to use only cool blue/indigo/violet/cyan/slate/emerald/rose UI color.
- In-app Browser QA: PASS for live token verification and runtime computed-color audit with `flaggedCount = 0`.

## Remaining Issues

- No frontend color or browser QA blocker remains for the current local run.
- Stitch design-system asset creation remains an external MCP blocker from prior work; the local frontend design system is implemented in repo docs and CSS tokens.
