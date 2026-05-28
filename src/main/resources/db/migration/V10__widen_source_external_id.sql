-- Geoapify place_id values can exceed 120 characters.
-- Widen source_external_id columns to 500 to accommodate all providers.

ALTER TABLE hotels
    ALTER COLUMN source_external_id TYPE VARCHAR(500);

ALTER TABLE hotel_images
    ALTER COLUMN source_external_id TYPE VARCHAR(500);

ALTER TABLE hotel_source_records
    ALTER COLUMN external_id TYPE VARCHAR(500);
