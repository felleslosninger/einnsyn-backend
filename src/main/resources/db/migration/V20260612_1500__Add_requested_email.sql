-- Staging field for an email address that has been requested but not yet verified
ALTER TABLE bruker
    ADD COLUMN IF NOT EXISTS requested_email TEXT COLLATE "C";

-- The single "secret"/"secret_expiry" is split into a dedicated password-reset secret
-- and a separate email-verification secret. Renaming keeps the existing unique index.
ALTER TABLE bruker
    RENAME COLUMN secret TO reset_password_secret;

ALTER TABLE bruker
    RENAME COLUMN secret_expiry TO reset_password_secret_expiry;

ALTER TABLE bruker
    ADD COLUMN IF NOT EXISTS validate_email_secret TEXT COLLATE "C";

ALTER TABLE bruker
    ADD COLUMN IF NOT EXISTS validate_email_secret_expiry timestamp with time zone;

CREATE UNIQUE INDEX IF NOT EXISTS idx_bruker_validate_email_secret
    ON bruker (validate_email_secret);
