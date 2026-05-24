# Dataset Import Report

## Source Decision

- Preferred source: OpenStreetMap accommodation/hotel data through Overpass API.
- License: ODbL; attribution required.
- Current implementation: synthetic fallback seed data in `DataSeeder` and `data/processed/synthetic_hotels.csv`.

## Reason for Fallback

- The app must run offline without requiring a network call or Railway credentials.
- OSM/Overpass scripts are provided for future legal open-data extraction, but not executed in this verification run.

## Imported Rows

- Local H2 seed at startup:
  - Hotels: 3
  - Rooms: 5
  - Amenities: 4
  - Images: 5 local SVG placeholders

## Verification

- `.\gradlew.bat test`: PASS. Tests render home/search and create a pending booking from seeded data.

## Browser Note

- HTTP QA fallback passed against the local app. Browser automation remains blocked because the in-app browser backend is unavailable in this session.
