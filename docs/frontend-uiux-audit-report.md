# Front-end UI/UX Audit Report

## Overview
Audited the currently deployed Railway site before the new redesign. The deployed site still used the old HotelBooking brand and a dark/teal hotel booking treatment that did not match the downloaded Stitch platinum reference.

## Deployed URL
https://hotelbooking-production-57a9.up.railway.app

## Pages Audited
- Home: `/`
- Password login: `/login/password`
- OTP login request: `/login/otp`
- Register: `/register`
- Room listing/search: `/rooms/search?checkIn=2026-05-28&checkOut=2026-05-30&guests=2`
- Admin route: `/admin`
- Error route: `/missing-page-for-audit`

Viewports audited: 1440, 1024, 768, 390.

Screenshots and raw audit JSON: `docs/qa-screenshots/lumiere-audit-before/`.

## Header Behavior Audit
- Header links on deployed site scrolled to `#rooms`, `#ai-recommendation`, `#hanoi`, `#tphcm`, and `#danang`.
- Smooth scroll worked on desktop audit.
- Header branding still showed `HotelBooking`.
- Header visual style did not match Stitch: reference expects light platinum header, center nav, black pill CTA.

## Login Separation Audit
- Password login page: `/login/password`
- OTP login page: `/login/otp`
- Are they separated: YES
- Issues: Both pages were visually close to old brand and did not match the Stitch login reference.

## Global Issues
- Brand still said `HotelBooking`.
- Favicon/logo used old blue `H` mark.
- Visual direction was dark and teal; Stitch folder requires white/silver/platinum.
- Previous redesign used a yellow/gold-like luxury accent and dark hotel photo hero, which conflicts with the requested Stitch reference.
- Header and hero composition did not match the downloaded Stitch file.
- Cards/forms/buttons did not consistently follow the platinum design system.

## Page-by-page Findings

### Page: Home
- URL: `/`
- Current problems: Old brand, dark hero, photo-first layout, search panel not like Stitch reference.
- UX problems: Header labels did not match the Stitch reference direction.
- Visual problems: Dark theme and saturated teal accents instead of soft platinum.
- Responsive problems: Mobile was usable but not aligned to the new design language.
- JS/console problems: None critical in audit.
- Network problems: None critical in audit.
- Priority: High.
- Recommended redesign direction: Replace with LUMIÈRE HOTEL platinum hero, abstract silver background, key visual, glass pill search, and curated room cards.

### Page: Password Login
- URL: `/login/password`
- Current problems: Old brand and old auth panel styling.
- UX problems: Password flow separate and usable.
- Visual problems: Did not match Stitch split auth layout.
- Responsive problems: Needs stronger mobile composition.
- JS/console problems: None critical in audit.
- Network problems: None critical in audit.
- Priority: High.
- Recommended redesign direction: Keep route/action/fields, apply split luxury auth layout.

### Page: OTP Login
- URL: `/login/otp`
- Current problems: Old brand and old auth panel styling.
- UX problems: OTP flow separate and usable.
- Visual problems: Did not match Stitch split auth layout.
- Responsive problems: Needs stronger mobile composition.
- JS/console problems: None critical in audit.
- Network problems: None critical in audit.
- Priority: High.
- Recommended redesign direction: Keep route/action/fields, apply same design system as password login.

### Page: Register
- URL: `/register`
- Current problems: Old brand and old form design.
- UX problems: Form fields are correct.
- Visual problems: Not platinum/luxury.
- Responsive problems: Long form needs clean vertical rhythm.
- JS/console problems: None critical in audit.
- Network problems: None critical in audit.
- Priority: Medium.
- Recommended redesign direction: Use same split auth shell and pill fields.

### Page: Room Listing
- URL: `/rooms/search`
- Current problems: Old cards/search toolbar.
- UX problems: Functional filters present.
- Visual problems: Cards lack the Stitch rounded platinum card language.
- Responsive problems: Table/list density should remain usable.
- JS/console problems: None critical in audit.
- Network problems: None critical in audit.
- Priority: Medium.
- Recommended redesign direction: Use white cards, soft shadows, pill controls.

## Design System Problems
- Old system mixed dark surfaces, teal, gold/yellow-like accents, and inconsistent radii.
- Typography did not follow the Stitch Montserrat direction.
- Hero/search/card/form/table styles lacked a single platinum token system.

## Recommended Global Redesign Strategy
- Replace active CSS with a new `lumiere.css` design system.
- Keep backend routes and form field names intact.
- Rebrand visible frontend to `LUMIÈRE HOTEL`.
- Replace favicon with platinum L/È monogram.
- Preserve header hash scrolling and mobile menu behavior.
- Keep login password and OTP as separate pages.

## Must-fix Checklist
- [x] Replace visible brand.
- [x] Replace favicon/logo.
- [x] Replace old active CSS entry point.
- [x] Redesign home to match Stitch folder reference.
- [x] Redesign login password separately.
- [x] Redesign login OTP separately.
- [x] Preserve header scroll-to-section behavior.
- [x] Build before push.
- [ ] Push and verify deployed Railway version.
