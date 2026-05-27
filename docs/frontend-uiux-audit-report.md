# Front-end UI/UX Audit Report

## Overview
Audit ran directly against the Railway deployed web app using Chrome DevTools MCP. Local UI was not used for visual verification.

## Deployed URL
https://hotelbooking-production-57a9.up.railway.app

## Pages Audited
- Home: `/`
- Password/OTP login: `/login`, `/login/password`, `/login/otp`
- Register: `/register`
- Room listing/search: `/rooms/search?checkIn=2026-05-28&checkOut=2026-05-30&guests=2`
- Room detail: `/rooms/{id}`
- Admin/user/booking pages: access requires authenticated user, so only route behavior was considered unauthenticated.
- Error page: `/login/otp` produced the deployed error page.

## Header Behavior Audit
- Desktop header renders at the top and hash links scroll to sections.
- Issue: hash scroll lands the section at the viewport top, which can be covered by the sticky header.
- Issue: `Tìm phòng` was route-based on the deployed version instead of a same-page section scroll.
- Issue: mobile/tablet header expanded all links in multiple rows; no compact open/close menu was visible on 768px and 390px.
- Required fix: keep login/register/admin/profile routes, but make public nav section items scroll smoothly to home sections.

## Login Separation Audit
- Password login page: `/login/password` renders a separate password form.
- OTP login page: `/login` renders the OTP request form.
- Are they separated: partially, but `/login/otp` returned 404 on deployed.
- Issues: `/login/otp` must be added as a real GET route, and `/login` must stay OTP-only instead of containing a hidden or secondary password form.

## Global Issues
- Home page had a CSP console error from inline script execution on deployed.
- Mobile nav was not compact.
- Current visual system was inconsistent between pending local CSS and deployed CSS.
- Some color overrides introduced yellow/gold in local CSS; this violates the redesign requirement and must be removed from the bundle.
- Some heading CSS used negative letter spacing; this was removed.

## Page-by-page Findings

### Page: Home
- URL: `/`
- Current problems: public nav did not fully preserve same-page scroll behavior; inline script CSP error appeared.
- UX problems: mobile nav consumed too much vertical space.
- Visual problems: hero/search area was improved but still needed a cleaner design system and non-yellow token set.
- Responsive problems: 768px and 390px showed full nav links instead of a drawer.
- JS/console problems: CSP blocked inline script.
- Network problems: no broken critical assets found.
- Priority: High.
- Recommended redesign direction: premium navy/teal system, external JS only, compact drawer, section-aware active nav.

### Page: Login Password
- URL: `/login/password`
- Current problems: separate page exists.
- UX problems: failure redirects should return here, not to OTP login.
- Visual problems: form styling needs to match new global system.
- Responsive problems: no blocking issue found, but auth panel needs better consistency.
- JS/console problems: none found.
- Network problems: none found.
- Priority: High.
- Recommended redesign direction: dedicated password page with clear link to OTP.

### Page: Login OTP
- URL: `/login` and `/login/otp`
- Current problems: `/login/otp` returned 404.
- UX problems: OTP entry and OTP verification should be distinct but connected.
- Visual problems: form styling needs global token cleanup.
- Responsive problems: inherits nav problem.
- JS/console problems: none on `/login`; `/login/otp` rendered error page.
- Network problems: none found.
- Priority: High.
- Recommended redesign direction: `/login` and `/login/otp` render OTP entry, verification template remains after request.

### Page: Register
- URL: `/register`
- Current problems: DevTools reported missing autocomplete hints on two inputs.
- UX problems: needs tighter account-page consistency.
- Visual problems: acceptable but should use new tokens.
- Responsive problems: inherits nav problem.
- JS/console problems: autocomplete warnings only.
- Network problems: none found.
- Priority: Medium.
- Recommended redesign direction: keep fields/actions, refine form controls and focus states.

### Page: Room Listing
- URL: `/rooms/search?checkIn=2026-05-28&checkOut=2026-05-30&guests=2`
- Current problems: listing renders many repeated rows; density and hierarchy need stronger table/card language.
- UX problems: filter toolbar is functional but visually heavy on small screens.
- Visual problems: repeated placeholder hotel data needs stronger card scanning.
- Responsive problems: inherits nav problem.
- JS/console problems: none found.
- Network problems: no broken critical assets found.
- Priority: Medium.
- Recommended redesign direction: image-led rows, compact chips, clearer price/action column.

### Page: Room Detail
- URL: `/rooms/{id}`
- Current problems: layout renders.
- UX problems: sticky booking box must remain usable on desktop and stack on mobile.
- Visual problems: needs consistent panel/card treatment.
- Responsive problems: requires final deployed check after refactor.
- JS/console problems: none found.
- Network problems: no broken critical assets found.
- Priority: Medium.
- Recommended redesign direction: large media, concise room details, elevated reserve panel.

## Design System Problems
- Yellow/gold appeared in local `luxury.css` tokens and was not allowed.
- Button/card/form/table styles were split across many files with competing overrides.
- Navigation behavior was split between CSS and missing deployed JS.
- Motion should be subtle and external-script/CSP-safe.

## Recommended Global Redesign Strategy
- Use Stitch Modern Luxury Travel direction with deep navy, slate, white, soft blue and teal/cyan accent.
- Keep a light premium interface with high-quality room imagery and quiet glass panels.
- Remove yellow from primary token path.
- Keep all backend routes, form actions, field names and security processing intact.
- Implement smooth anchor scrolling in `nav.js`, with mobile drawer and active section state.

## Must-fix Checklist
- [x] Remove yellow/gold CSS from active bundle.
- [x] Add `/login/otp` GET route.
- [x] Keep password and OTP pages separate.
- [x] Route password login failures to `/login/password`.
- [x] Make public nav items same-page anchors.
- [x] Add smooth scroll with sticky-header offset.
- [x] Ensure mobile nav opens/closes.
- [x] Remove inline-script dependency from home behavior.
- [ ] Build and deploy.
- [ ] Verify final UI on Railway.
