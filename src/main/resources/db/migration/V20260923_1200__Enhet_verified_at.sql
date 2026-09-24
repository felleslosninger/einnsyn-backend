ALTER TABLE enhet ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone;

-- Everything that exists today was created by an admin or a verified Enhet.
UPDATE enhet SET verified_at = COALESCE(_created, now()) WHERE verified_at IS NULL;
