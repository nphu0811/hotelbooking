# Hotel Data Source Decision

Last updated: 2026-05-25

## Summary

The MVP production source is OpenStreetMap through Overpass API. It provides real accommodation place data without paid credentials, with clear ODbL attribution obligations. It does not provide live availability, room inventory, or rates, so imported hotels get internal room/rate templates explicitly marked as estimates.

## Source Comparison

| Source | Access | Data Available | Store In DB? | Rate Limit / Usage | Legal / Terms Risk | Env Vars |
| --- | --- | --- | --- | --- | --- | --- |
| OpenStreetMap / Overpass API | Free public endpoint; no key | Real OSM elements with `tourism=hotel/guest_house/hostel/apartment/chalet/resort`, name, tags, coordinates, address tags, phone, website, stars when mapped | Yes, under ODbL obligations with attribution and share-alike awareness | Public Overpass guidance says about 10,000 requests/day and under about 1 GB/day as a broad safe margin; heavy app backends should run their own instance | Medium: must attribute OSM, respect ODbL, avoid overusing public instances | `HOTELDATA_OVERPASS_ENDPOINT` optional |
| Geoapify Places API | API key; optional enrichment | Accommodation POIs by city bounding box, formatted address, contact, categories; often links back to OSM datasource | Yes with provenance (`GEOAPIFY`, `place_id`, `source_url`, `raw_payload`, sync timestamps); dedupe against Overpass by name + proximity | Plan-based; importer pauses between pages and surfaces HTTP 429 | Medium: follow Geoapify terms and attribution; disabled by default for MVP | `GEOAPIFY_API_KEY`, `GEOAPIFY_ENABLED` |
| Google Places API | Paid/billing API key; optional, disabled by default | Place details, formatted address, rating, photos, reviews, attributions | Restricted. Place ID can be stored indefinitely; most Places content must not be prefetched/cached/stored outside allowed exceptions | Paid quota/billing controlled by Google Cloud project | High: strict caching, map display, Google logo, third-party/photo/review attribution requirements; billing may block enablement | `GOOGLE_PLACES_API_KEY`, `GOOGLE_PLACES_ENABLED` |
| Amadeus Hotel APIs | Self-service key; production access for real-time data | Hotel list/search/offers/booking/ratings/name autocomplete depending API | Store only according to Amadeus terms; offers/rates should be TTL cached, not treated as static hotel facts without terms review | Official guide lists 10 TPS test and 40 TPS production for most self-service APIs | Medium/high: credentials required; test data limited/cached; production needed for real-time data | `AMADEUS_CLIENT_ID`, `AMADEUS_CLIENT_SECRET` |
| Expedia Rapid API | Partner access only; applications reviewed | Large lodging inventory, property/room content, rates, availability, booking | Only with partner agreement | Partner/consultant governed; not public no-key access | High: partner contract required; do not implement without access | Partner-issued Rapid credentials |
| Local/open datasets | Varies | Depends on dataset | Only if license allows commercial use, storage, redistribution, and provenance | Dataset-specific | Unknown until specific source reviewed | Source-specific |

## Official Source Notes

- OSM data is ODbL and requires OpenStreetMap/contributor attribution plus making clear the data is available under ODbL: https://www.openstreetmap.org/copyright
- Overpass public instances are shared infrastructure; public guidance warns against using them as a high-demand application backend and gives broad safety margins of about 10,000 requests/day and under about 1 GB/day: https://dev.overpass-api.de/overpass-doc/en/preface/commons.html
- Google Places policies prohibit storing/caching most Places content beyond allowed exceptions, while place IDs are exempt; Google/third-party/photo/review attributions are required: https://developers.google.com/maps/documentation/places/web-service/policies
- Amadeus test vs production docs state test data is limited/cached and production is needed for real-time data: https://amadeus4dev.github.io/developer-guides/test-data/
- Amadeus rate-limit guide lists 10 TPS test and 40 TPS production for most self-service APIs: https://amadeus4dev.github.io/developer-guides/api-rate-limits/
- Expedia Rapid is partner-gated and applications are reviewed case by case: https://partner.expediagroup.com/en-us/join-us/rapid-api

## Selected MVP Production Source

Selected: OpenStreetMap / Overpass API.

Reasons:

- Legal no-key source for real place data.
- Sufficient for MVP hotel discovery: name, approximate address tags, latitude/longitude, contact tags, website, stars when mapped.
- Provenance can be stored per OSM element ID and linked back to `openstreetmap.org/{type}/{id}`.
- No scraping of OTA websites.
- Allows a production portfolio to show real place data while explicitly labeling rooms/rates as internal estimates.

## Optional Enrichment Sources

- **Geoapify Places API**: enabled with `GEOAPIFY_ENABLED=true`. Imports accommodations inside the same city bounding boxes as Overpass, stores `source=GEOAPIFY`, and deduplicates against existing Overpass hotels. See `docs/geoapify-setup.md`.
- **Google Places API**: enabled with `GOOGLE_PLACES_ENABLED=true`. Not used for MVP while billing blocks API access. See `docs/hotel-data-import-google-places.md`.

## Fallback Source

Fallback: local fixture files derived from previously imported OSM records, stored with OSM attribution and source payload hashes. Use only for tests or offline development, not to claim fresh/live data.

## Fields Stored

- Hotel identity: name, slug, city, province, country.
- Location: latitude, longitude.
- Contact/provenance: phone, website, source, source external ID, source URL.
- Quality: data quality score, imported/synced timestamps.
- Raw source payload in `hotel_source_records.raw_payload` for OSM/Overpass provenance.

## Fields Not Claimed

- Live availability.
- Provider-confirmed room inventory.
- Provider-confirmed rates.
- Google Places ratings/photos/reviews unless a future implementation satisfies Google caching and attribution rules.

## Internal Room/Rate Template Rule

When a hotel source does not provide rooms or rates, the importer creates one room template only so the existing room-based search flow remains usable. These records are explicitly marked:

- `room_source=INTERNAL_TEMPLATE`
- `rate_source=INTERNAL_ESTIMATE`

They must not be presented as live hotel inventory or real provider pricing.
