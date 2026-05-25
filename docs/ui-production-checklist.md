# UI Production Checklist

## Completed

- Shared Thymeleaf layout fragments added for `head`, `footer`, `alerts`, and `scripts`.
- Primary customer pages use the shared head/footer fragments.
- Search form has explicit ARIA labels for date, guests, sort, and the form region.
- Search and detail cards use a local no-image fallback when `primaryImageUrl` is missing.
- Imported hotel data shows source attribution and links to source/map records when available.
- Empty states remain visible for search results, reviews, and booking history.
- Login page includes a rate-limited verification resend form.
- Playwright smoke coverage added for home, search, detail, login, register, and protected admin redirect on desktop and mobile Chromium.

## Remaining UI Risks

- Admin pages still use older table-oriented templates and should be moved to the shared head/footer fragments in a later cleanup.
- Full authenticated browser booking with payment requires a local-only e2e user bootstrap or a verified account fixture; this is intentionally not seeded in production.
- CSS is not minified by the Gradle build yet. Static assets are cacheable but not fingerprinted.

## Verification

```bash
./gradlew test
npm run test:e2e -- --reporter=line --workers=1
```
