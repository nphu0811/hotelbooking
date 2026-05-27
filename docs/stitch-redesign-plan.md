# Stitch Redesign Plan

## Design Goal
Create a modern premium hotel booking experience for Vietnamese travelers: polished, calm, responsive, and production-ready. The direction comes from MCP Stitch project `5571804125666173906`, whose generated design theme is "Modern Luxury Travel".

## Global Design System
- Palette: deep navy `#0B1F33`, slate, white, soft blue `#EAF4FF`, teal/cyan `#11B5C8`, restrained blue accent. Yellow is not a primary color.
- Typography: Inter/system UI for body and UI controls; strong display type only for true hero contexts. Letter spacing remains `0`.
- Spacing: 8px rhythm, larger section gaps, compact controls in dense admin areas.
- Radius: controlled 8-16px card/panel radius; no bubbly oversized shapes for operational controls.
- Shadow: navy-tinted ambient shadows, subtle lift on hover.
- Buttons: navy-to-teal primary gradient, quiet white/glass secondary buttons, clear disabled/loading states.
- Forms: visible labels, strong focus ring, validation messaging, no placeholder-only fields.
- Cards: image-first room cards/rows with compact metadata.
- Tables: dense admin tables, sticky-feeling header styling, row hover and status badges.
- Badge/status: small uppercase chips for amenity/source/status.
- Navigation/header: sticky glass topbar, desktop same-page scroll links, mobile drawer.

## Header Redesign
- Must preserve scroll-to-section behavior: public nav items scroll to home sections.
- Desktop behavior: `Tìm phòng -> #rooms`, `Gợi ý AI -> #ai-recommendation`, city links to city sections.
- Mobile behavior: hamburger opens/closes an animated drawer; links close drawer after click.
- Active state: scroll spy marks current section with background and underline.
- Sticky behavior: header backdrop/shadow strengthens after scroll; anchor scrolling offsets header height.

## Auth Pages
### Password Login Page
- Route: `/login/password`.
- Action remains Spring Security `POST /login` with `username` and `password`.
- Failure redirects back to password page.
- Cross-link to OTP page.

### OTP Login Page
- Routes: `/login`, `/login/otp`, `/login-otp`.
- OTP request posts to `/login/otp/request`.
- OTP verification remains `auth/login-otp.html` after request and posts `/login/otp/verify`.
- Cross-link to password page.

### Register Page
- Preserve route/action and field names.
- Match form styling, focus states and OAuth button style.

## Home Page
- Hero has `#home`, premium hotel imagery and elevated search panel.
- Featured rooms section has `#rooms` for header scroll.
- AI recommendation section remains external-JS driven to avoid CSP violations.
- City sections keep `#hanoi`, `#tphcm`, `#danang`.

## Room Listing Page
- Keep search action and query params.
- Apply global toolbar, room row/card, chips and price/action styling.

## Room Detail Page
- Preserve booking form action.
- Use media-led layout and sticky reserve panel.

## Booking/Payment Pages
- Preserve booking/payment routes and provider form values.
- Style hold countdown, summary cards, payment method cards and results consistently.

## User Pages
- Profile and booking history use the same panel/form/table system.
- Verification controls use compact status badges.

## Admin Pages
- Dashboard uses metric cards, compact operation panels and dense tables.
- Room/booking/user management keeps actions/forms but improves scan hierarchy.

## Animation Plan
- Hero intro with subtle entrance.
- Header transition on scroll.
- Smooth section scroll with header offset.
- Search focus glow.
- Room card hover lift and image zoom.
- Button hover/press.
- Form validation state.
- Drawer slide/fade.
- Admin card entrance and row hover.
- Skeleton/empty state styles remain lightweight.
- Respect reduced motion.

## Responsive Plan
- 1440px: full desktop grid/topbar.
- 1024px: compact topbar and stacked hero/search as needed.
- 768px: hamburger drawer, stacked forms/cards.
- 390px: single-column layout, full-width buttons, no text overflow.

## Accessibility Plan
- Visible focus states on all interactive controls.
- Labels remain attached to inputs.
- Header drawer uses `aria-expanded`, `aria-controls`, Escape close and outside-click close.
- Links/buttons keep sufficient contrast.
- Room images keep `alt`.

## Implementation Checklist
- [x] Remove active `luxury.css` import and yellow primary token path.
- [x] Update tokens to navy/teal system.
- [x] Refactor nav hrefs to section anchors.
- [x] Add active scroll spy and sticky state in external JS.
- [x] Add `/login/otp` route and keep auth pages separate.
- [x] Route password failures to password page.
- [x] Bump cache versions for CSS/JS.
- [ ] Build.
- [ ] Push.
- [ ] Railway deploy verification.
