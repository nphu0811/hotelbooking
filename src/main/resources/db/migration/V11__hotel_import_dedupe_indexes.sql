CREATE INDEX IF NOT EXISTS idx_hotels_lower_name_lat_lng
    ON hotels (lower(name), latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_hotels_source_upper_lower_name_lat_lng
    ON hotels (upper(source), lower(name), latitude, longitude);
