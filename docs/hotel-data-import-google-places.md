# Hotel Data Import with Google Places (optional, disabled by default)

Google Places is **not** part of the MVP import path. Billing/prepayment may block API enablement. The provider stays available for future enrichment only when explicitly enabled.

This project can import real hotel place records from Google Places API (New) and create internal room templates for booking tests when you opt in.

## Required API

Enable these Google Maps Platform APIs for the same key:

- Places API (New)
- Place Photos (New)

The provider uses:

- `places:searchText` for hotel discovery
- `places.photos.getMedia` with `skipHttpRedirect=true` to resolve a displayable photo URL

## Environment

Do not hard-code the key. Set environment variables:

```powershell
$env:GOOGLE_PLACES_ENABLED="true"
$env:GOOGLE_PLACES_API_KEY="your-key"
```

or in Railway:

```text
GOOGLE_PLACES_ENABLED=true
GOOGLE_PLACES_API_KEY=your-key
```

With `GOOGLE_PLACES_ENABLED=false` (default), `--source=google_places` fails fast with a clear message. Use `--source=overpass` for MVP imports, or `--source=geoapify` when Geoapify is enabled (see `docs/geoapify-setup.md`).

## Import command

Dry run first:

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Da Nang' --limit=40 --dry-run --app.import-hotels.exit"
```

Then import:

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Da Nang' --limit=40 --app.import-hotels.exit"
```

Repeat by city for broader coverage:

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Ho Chi Minh City' --limit=40 --app.import-hotels.exit"
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Ha Noi' --limit=40 --app.import-hotels.exit"
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Da Nang' --limit=40 --app.import-hotels.exit"
.\gradlew.bat bootRun --args="--app.import-hotels --source=google_places --city='Nha Trang' --limit=40 --app.import-hotels.exit"
```

## Data model behavior

- Real hotel metadata is stored in `hotels`.
- Source audit metadata is stored in `hotel_source_records`.
- Each imported hotel gets one internal `Standard Room` template so the booking UI has inventory.
- If Google returns a photo, the resolved `photoUri` is used as the room primary image.
- API keys are never stored in the database or rendered to the client.

## Policy guardrails

Google photo names are not persisted because Google warns that photo names can expire and must not be cached. The stored source payload is sanitized and does not include the raw photo resource name. Re-run import to refresh image URLs when needed.

