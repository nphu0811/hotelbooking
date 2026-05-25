# Database Performance Notes

## Query Changes

- Room search now pages room IDs first, then fetches the detailed graph by ID list. This avoids applying pagination directly to collection-fetch/entity-graph results.
- Admin booking pages use a dedicated booking query with `room`, `room.hotel`, and `user` entity graph to reduce N+1 query risk.

## Indexes / Constraints

- `uq_users_email_lower` enforces normalized email uniqueness at the database layer.
- Hotel source/external ID indexes support idempotent real-data imports.
- Hotel city/province and lat/lng indexes support search and future map/geospatial filtering.
- Room source/rate source indexes make it easy to audit internal estimates vs provider data.
- Latitude, longitude, and data-quality score check constraints protect imported source quality.

## Migration Verification

- `MigrationPostgresTest` runs Flyway migrations on PostgreSQL with Testcontainers when Docker is available.
- H2 remains a local/test convenience, not proof of production schema compatibility.
