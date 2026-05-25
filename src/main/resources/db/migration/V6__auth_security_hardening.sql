ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS email_verification_last_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_email_verification_token_hash
    ON users(email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_email_hash_length;

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_hash_length
    CHECK (email_verification_token_hash IS NULL OR char_length(email_verification_token_hash) = 64);
