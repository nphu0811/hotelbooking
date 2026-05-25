ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(120);

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS chk_payments_status;

UPDATE payments SET status = 'PENDING' WHERE status = 'INITIATED';
UPDATE payments SET status = 'PAID' WHERE status = 'SUCCESS';
UPDATE payments SET status = 'CANCELLED' WHERE status = 'TIMEOUT';

ALTER TABLE payments
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
    CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED'));

CREATE TABLE payment_webhook_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(40) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL UNIQUE,
    payment_id UUID REFERENCES payments(payment_id) ON DELETE SET NULL,
    order_id VARCHAR(80),
    event_status VARCHAR(40) NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    raw_payload JSONB,
    error_message TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_webhook_events_status
        CHECK (event_status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED'))
);

CREATE INDEX idx_payment_webhook_events_payment ON payment_webhook_events(payment_id);
CREATE INDEX idx_payment_webhook_events_provider_received ON payment_webhook_events(provider, received_at DESC);
CREATE INDEX idx_payment_webhook_events_processed ON payment_webhook_events(processed, received_at);

ALTER TABLE email_jobs
    ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(255);

ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(255);

ALTER TABLE email_jobs
    DROP CONSTRAINT IF EXISTS chk_email_jobs_status;

UPDATE email_jobs SET status = 'RETRYING' WHERE status = 'SENDING';

ALTER TABLE email_jobs
    ADD CONSTRAINT chk_email_jobs_status
    CHECK (status IN ('PENDING', 'RETRYING', 'SENT', 'FAILED', 'BOUNCED', 'COMPLAINED'));

ALTER TABLE refund_requests
    DROP CONSTRAINT IF EXISTS chk_refund_requests_status;

UPDATE refund_requests SET status = 'SUCCEEDED' WHERE status = 'SUCCESS';

ALTER TABLE refund_requests
    ADD CONSTRAINT chk_refund_requests_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED'));
