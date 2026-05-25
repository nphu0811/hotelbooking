ALTER TABLE hotels
    ADD COLUMN IF NOT EXISTS slug VARCHAR(180),
    ADD COLUMN IF NOT EXISTS address_line VARCHAR(500),
    ADD COLUMN IF NOT EXISTS country VARCHAR(80) NOT NULL DEFAULT 'VN',
    ADD COLUMN IF NOT EXISTS star_rating INTEGER,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS website VARCHAR(500),
    ADD COLUMN IF NOT EXISTS source VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_external_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS data_quality_score INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS imported_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMPTZ;

UPDATE hotels SET address_line = address WHERE address_line IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_hotels_source_external_id
    ON hotels(source, source_external_id)
    WHERE source IS NOT NULL AND source_external_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_hotels_lat_lng ON hotels(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_hotels_source ON hotels(source);

CREATE TABLE IF NOT EXISTS hotel_images (
    hotel_image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(hotel_id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    source VARCHAR(40) NOT NULL,
    source_external_id VARCHAR(120),
    attribution VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS hotel_amenities (
    hotel_amenity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(hotel_id) ON DELETE CASCADE,
    amenity_code VARCHAR(120) NOT NULL,
    amenity_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_hotel_amenities_code UNIQUE (hotel_id, amenity_code)
);

CREATE TABLE IF NOT EXISTS hotel_source_records (
    source_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(40) NOT NULL,
    external_id VARCHAR(120) NOT NULL,
    raw_payload JSONB,
    payload_hash VARCHAR(64) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_hotel_source_records_source_external UNIQUE (source, external_id)
);

CREATE TABLE IF NOT EXISTS hotel_import_runs (
    import_run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    total_fetched INTEGER NOT NULL DEFAULT 0,
    total_inserted INTEGER NOT NULL DEFAULT 0,
    total_updated INTEGER NOT NULL DEFAULT 0,
    total_skipped INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    CONSTRAINT chk_hotel_import_runs_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'DRY_RUN'))
);

CREATE TABLE IF NOT EXISTS api_rate_limit_state (
    source VARCHAR(40) PRIMARY KEY,
    next_allowed_at TIMESTAMPTZ,
    last_status_code INTEGER,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS room_source VARCHAR(60) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS rate_source VARCHAR(60) NOT NULL DEFAULT 'MANUAL';
