ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS phone_verification_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS phone_verification_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS phone_verification_last_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS login_otp_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS login_otp_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS login_otp_last_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_phone
    ON users(phone)
    WHERE phone IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_phone_verification_token_hash
    ON users(phone_verification_token_hash)
    WHERE phone_verification_token_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_login_otp_token_hash
    ON users(login_otp_token_hash)
    WHERE login_otp_token_hash IS NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_phone_verification_hash_length,
    DROP CONSTRAINT IF EXISTS chk_users_login_otp_hash_length;

ALTER TABLE users
    ADD CONSTRAINT chk_users_phone_verification_hash_length
    CHECK (phone_verification_token_hash IS NULL OR char_length(phone_verification_token_hash) = 64),
    ADD CONSTRAINT chk_users_login_otp_hash_length
    CHECK (login_otp_token_hash IS NULL OR char_length(login_otp_token_hash) = 64);
