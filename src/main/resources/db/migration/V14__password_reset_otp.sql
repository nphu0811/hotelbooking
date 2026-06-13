ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS password_reset_last_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token_hash
    ON users(password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_password_reset_hash_length;

ALTER TABLE users
    ADD CONSTRAINT chk_users_password_reset_hash_length
    CHECK (password_reset_token_hash IS NULL OR char_length(password_reset_token_hash) = 64);
