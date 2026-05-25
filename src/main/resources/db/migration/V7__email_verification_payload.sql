ALTER TABLE email_jobs
    ADD COLUMN IF NOT EXISTS body_text TEXT;
