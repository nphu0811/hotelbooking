# Stitch Redesign Plan

## Design Goal
Apply the downloaded Stitch folder direction only, not MCP Stitch: `Modern Platinum Luxury` for `LUMIÈRE HOTEL`.

The target UI is light, white/silver/platinum, soft minimalist, with a black onyx CTA, pill search bar, rounded glass panels, and restrained motion.

## Global Design System
- Brand: `LUMIÈRE HOTEL`.
- Logo/favicon: silver-white L/È monogram on deep onyx.
- Colors: white `#ffffff`, porcelain `#f8f9fa`, platinum `#e5e4e2`, silver `#c8c6c5`, onyx `#000000`.
- Typography: Montserrat-style stack with local fallback because CSP blocks external font CDNs.
- Shapes: pill inputs/buttons; 24-32px rounded cards to match Stitch folder.
- Motion: subtle hover lift and floating key animation only.
- No yellow as primary color.

## Header Redesign
- Must preserve scroll-to-section behavior: YES.
- Desktop behavior: brand left, center nav, auth/book-now actions right.
- Mobile behavior: compact hamburger panel with smooth open/close.
- Active state: underline on current hash section.
- Sticky behavior: sticky top header with light glass background.

## Auth Pages
### Password Login Page
- Keep `/login/password`.
- Keep form action `/login`.
- Keep fields `username` and `password`.
- Apply split image/form layout from Stitch login reference.

### OTP Login Page
- Keep `/login`, `/login/otp`, `/login-otp` request route.
- Keep request form action `/login/otp/request` and field `identifier`.
- Keep verify form action `/login/otp/verify` and fields `identifier`, `otp`.
- Do not merge password and OTP into one page.

### Register Page
- Keep `/register`.
- Apply same auth shell and design tokens.

## Home Page
- Replace dark photo hero with Stitch-style platinum abstract hero.
- Add CSS-built key visual inspired by the Stitch 3D key.
- Use glass/pill search form.
- Keep real Thymeleaf room data.
- Keep section anchors for header scroll.

## Room Listing Page
- Keep backend query parameters and search action.
- Apply global white card/list/table styles.

## Room Detail Page
- Keep booking form action and fields.
- Apply global image, card, booking-box and chip styles.

## Booking/Payment Pages
- Keep payment provider form actions.
- Apply global summary/payment card styles.

## User Pages
- Profile inherits the same form and panel system.
- Booking history inherits table/badge/button system.

## Admin Pages
- Keep admin routes and data.
- Apply global glass panel, metric, table and status styles.

## Animation Plan
- Header and mobile menu: smooth open/close.
- Cards/buttons: gentle lift.
- Hero key: slow floating motion.
- Respect `prefers-reduced-motion`.

## Responsive Plan
- 1440/1024: full header layout and two-column hero/auth where applicable.
- 768: mobile nav panel, stacked hero/search.
- 390: single column, pill search becomes stacked panel.

## Accessibility Plan
- Keep semantic forms and labels.
- Preserve visible focus states.
- Keep navigation links real anchors/routes.
- Avoid text overlap through fixed responsive breakpoints.

## Implementation Checklist
- [x] Replace active CSS with `lumiere.css`.
- [x] Rebrand nav/footer/title to LUMIÈRE HOTEL.
- [x] Replace favicon with L/È monogram.
- [x] Redesign home from Stitch folder.
- [x] Redesign auth pages without merging login flows.
- [x] Preserve Spring Security form names/actions.
- [x] Build pass.
- [ ] Commit/push.
- [ ] Wait Railway deploy and verify deployed URL.
