-- Flyway migration: add user scoping and expiry to idempotency records

ALTER TABLE idempotency_record
    ADD COLUMN user_id VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE idempotency_record
    ADD COLUMN expires_at TIMESTAMP;

CREATE INDEX idx_idempotency_user_key ON idempotency_record(user_id, idempotency_key);
