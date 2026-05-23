CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token UUID,
    email_verification_expires_at TIMESTAMPTZ,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    last_failed_login_at TIMESTAMPTZ,
    locked_until TIMESTAMPTZ,
    lock_reason VARCHAR(500),
    email_invalid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED')),
    CONSTRAINT chk_users_failed_login_count CHECK (failed_login_count >= 0)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(role_id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE hotels (
    hotel_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    description TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rooms (
    room_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL REFERENCES hotels(hotel_id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    room_type VARCHAR(60) NOT NULL,
    description TEXT NOT NULL,
    capacity INTEGER NOT NULL,
    area_sqm NUMERIC(8, 2),
    price_per_night NUMERIC(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'AVAILABLE',
    cancellation_policy TEXT,
    average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rooms_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE')),
    CONSTRAINT chk_rooms_capacity CHECK (capacity BETWEEN 1 AND 10),
    CONSTRAINT chk_rooms_price CHECK (price_per_night > 0 AND price_per_night <= 100000000),
    CONSTRAINT chk_rooms_rating CHECK (average_rating >= 0 AND average_rating <= 5),
    CONSTRAINT chk_rooms_review_count CHECK (review_count >= 0)
);

CREATE TABLE room_images (
    image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE amenities (
    amenity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(80) NOT NULL UNIQUE,
    icon VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE room_amenities (
    room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities(amenity_id) ON DELETE RESTRICT,
    PRIMARY KEY (room_id, amenity_id)
);

CREATE TABLE bookings (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE RESTRICT,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT',
    price_per_night_snapshot NUMERIC(12, 2) NOT NULL,
    nights INTEGER NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    special_request VARCHAR(500),
    expires_at TIMESTAMPTZ,
    checked_in_at TIMESTAMPTZ,
    checked_out_at TIMESTAMPTZ,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT chk_bookings_guest_count CHECK (guest_count BETWEEN 1 AND 10),
    CONSTRAINT chk_bookings_nights CHECK (nights BETWEEN 1 AND 30),
    CONSTRAINT chk_bookings_snapshot_price CHECK (price_per_night_snapshot > 0),
    CONSTRAINT chk_bookings_total_amount CHECK (total_amount >= 0)
);

CREATE TABLE payments (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    provider VARCHAR(40) NOT NULL,
    order_id VARCHAR(80) NOT NULL UNIQUE,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(40) NOT NULL DEFAULT 'INITIATED',
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    request_payload JSONB,
    callback_payload JSONB,
    signature_valid BOOLEAN,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'TIMEOUT', 'REFUNDED')),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);

CREATE TABLE refund_requests (
    refund_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    payment_id UUID REFERENCES payments(payment_id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,
    percentage INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    reason VARCHAR(500),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refund_requests_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_refund_requests_amount CHECK (amount >= 0),
    CONSTRAINT chk_refund_requests_percentage CHECK (percentage IN (0, 50, 100))
);

CREATE TABLE reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE RESTRICT,
    rating INTEGER NOT NULL,
    cleanliness_rating INTEGER NOT NULL,
    service_rating INTEGER NOT NULL,
    location_rating INTEGER NOT NULL,
    value_rating INTEGER NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_cleanliness CHECK (cleanliness_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_service CHECK (service_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_location CHECK (location_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_value CHECK (value_rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_content_length CHECK (char_length(content) BETWEEN 50 AND 2000)
);

CREATE TABLE review_images (
    review_image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id UUID NOT NULL REFERENCES reviews(review_id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE email_jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID REFERENCES bookings(booking_id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    event_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(120) NOT NULL,
    payload JSONB,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_email_jobs_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'BOUNCED', 'COMPLAINED')),
    CONSTRAINT chk_email_jobs_attempts CHECK (attempts BETWEEN 0 AND 3)
);

CREATE TABLE email_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES email_jobs(job_id) ON DELETE SET NULL,
    booking_id UUID REFERENCES bookings(booking_id) ON DELETE SET NULL,
    event_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_email_logs_status CHECK (status IN ('SENT', 'FAILED', 'BOUNCED', 'COMPLAINED'))
);

CREATE TABLE audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    before_data JSONB,
    after_data JSONB,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE login_logs (
    login_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    email VARCHAR(255) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE jwt_token_blacklist (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    reason VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles(code, description) VALUES
    ('USER', 'Default customer role'),
    ('ADMIN', 'Hotel booking administrator'),
    ('SUPER_ADMIN', 'Can manage administrator roles')
ON CONFLICT (code) DO NOTHING;
