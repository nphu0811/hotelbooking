CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE INDEX idx_hotels_city_province ON hotels(city, province) WHERE is_deleted = FALSE;
CREATE INDEX idx_hotels_name_trgm ON hotels USING GIN (name gin_trgm_ops);
CREATE INDEX idx_hotels_address_trgm ON hotels USING GIN (address gin_trgm_ops);

CREATE INDEX idx_rooms_hotel_status ON rooms(hotel_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_rooms_price ON rooms(price_per_night) WHERE is_deleted = FALSE;
CREATE INDEX idx_rooms_capacity ON rooms(capacity) WHERE is_deleted = FALSE;
CREATE INDEX idx_rooms_name_trgm ON rooms USING GIN (name gin_trgm_ops);
CREATE INDEX idx_rooms_type_trgm ON rooms USING GIN (room_type gin_trgm_ops);
CREATE UNIQUE INDEX uq_rooms_hotel_name_active ON rooms(hotel_id, lower(name)) WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uq_room_images_primary ON room_images(room_id) WHERE is_primary = TRUE;

CREATE INDEX idx_bookings_user_created ON bookings(user_id, created_at DESC);
CREATE INDEX idx_bookings_room_dates ON bookings(room_id, check_in, check_out);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_check_in ON bookings(check_in);
CREATE INDEX idx_bookings_check_out ON bookings(check_out);
CREATE INDEX idx_bookings_pending_expiry ON bookings(expires_at) WHERE status = 'PENDING_PAYMENT';

ALTER TABLE bookings
    ADD CONSTRAINT ex_bookings_no_overlap
    EXCLUDE USING GIST (
        room_id WITH =,
        daterange(check_in, check_out, '[)') WITH &&
    )
    WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN'));

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status_created ON payments(status, created_at);

CREATE INDEX idx_refund_requests_booking ON refund_requests(booking_id);
CREATE INDEX idx_refund_requests_status ON refund_requests(status);

CREATE INDEX idx_reviews_room_created ON reviews(room_id, created_at DESC);
CREATE INDEX idx_reviews_user ON reviews(user_id);

CREATE INDEX idx_email_jobs_status_next_attempt ON email_jobs(status, next_attempt_at);
CREATE INDEX idx_email_logs_booking_created ON email_logs(booking_id, created_at DESC);

CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_login_logs_user_created ON login_logs(user_id, created_at DESC);
CREATE INDEX idx_login_logs_email_created ON login_logs(email, created_at DESC);

CREATE INDEX idx_jwt_blacklist_user ON jwt_token_blacklist(user_id);
CREATE INDEX idx_jwt_blacklist_expiry ON jwt_token_blacklist(expires_at);
