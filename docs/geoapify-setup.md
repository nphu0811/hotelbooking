# Geoapify Places API Setup

Geoapify is an **optional** hotel enrichment provider. When `GEOAPIFY_ENABLED=false` (the default), imports use **OpenStreetMap / Overpass only**. Google Places remains a separate optional provider and is also disabled by default.

## Environment variables

Set these in your local `.env` file or deployment environment. Do not commit real API keys to Git.

```properties
# Default: OSM/Overpass only for MVP imports
GEOAPIFY_ENABLED=false

# Required only when GEOAPIFY_ENABLED=true and using --source=geoapify
GEOAPIFY_API_KEY=YOUR_KEY
```

Spring maps them to:

- `geoapify.enabled` ← `GEOAPIFY_ENABLED`
- `geoapify.api-key` ← `GEOAPIFY_API_KEY`

## Supported cities (bounding boxes)

The provider searches accommodations inside pre-defined city bounding boxes aligned with Overpass import:

- Ho Chi Minh City
- Ha Noi
- Da Nang
- Da Lat
- Nha Trang
- Vung Tau
- Phu Quoc
- Hoi An
- Hue

## Import commands

Dry-run (fetches and deduplicates without writing hotels):

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=geoapify --city='Ho Chi Minh City' --limit=10 --dry-run --app.import-hotels.exit"
```

Live import (requires `GEOAPIFY_ENABLED=true` and `GEOAPIFY_API_KEY`):

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=geoapify --city='Ho Chi Minh City' --limit=10 --app.import-hotels.exit"
```

Default MVP import (Overpass only):

```powershell
.\gradlew.bat bootRun --args="--app.import-hotels --source=overpass --city='Ho Chi Minh City' --limit=100 --app.import-hotels.exit"
```

## Provenance stored in the database

Each imported hotel records:

| Field | Geoapify value |
| --- | --- |
| `source` | `GEOAPIFY` |
| `source_external_id` | Geoapify `place_id` |
| `source_url` | OSM URL from datasource when present |
| `raw_payload` | Sanitized feature JSON in `hotel_source_records` |
| `imported_at` | First import timestamp |
| `last_synced_at` | Updated on each sync |

## Deduplication

Geoapify rows are matched against existing **OVERPASS** hotels by **name + coordinate proximity** (~78 m window). When a match is found, the existing hotel row is updated instead of creating a duplicate.

## Rate limits

The provider paginates with `limit`/`offset`, uses a 500 ms pause between pages, and surfaces HTTP `429` as a clear import error. Respect your Geoapify plan quota; prefer dry-run first on new cities.

## Error logging

Failed import runs store a sanitized `error_message` in `hotel_import_runs`. API keys are redacted from logs and persisted error text.
