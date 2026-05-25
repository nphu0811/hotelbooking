CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower ON users(lower(email));

CREATE INDEX IF NOT EXISTS idx_hotels_province_city ON hotels(province, city) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_hotels_source_external ON hotels(source, source_external_id);
CREATE INDEX IF NOT EXISTS idx_rooms_rate_source ON rooms(rate_source);
CREATE INDEX IF NOT EXISTS idx_rooms_room_source ON rooms(room_source);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_order ON payment_webhook_events(order_id);

ALTER TABLE hotels
    ADD CONSTRAINT chk_hotels_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    ADD CONSTRAINT chk_hotels_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    ADD CONSTRAINT chk_hotels_data_quality CHECK (data_quality_score BETWEEN 0 AND 100);
