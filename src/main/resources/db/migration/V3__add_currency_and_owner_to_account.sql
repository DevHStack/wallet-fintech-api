-- Flyway migration: add currency and owner_id to account

ALTER TABLE account
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';

ALTER TABLE account
    ADD COLUMN owner_id VARCHAR(100) NOT NULL DEFAULT '';
