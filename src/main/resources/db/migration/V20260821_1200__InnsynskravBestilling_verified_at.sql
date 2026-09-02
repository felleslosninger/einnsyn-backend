ALTER TABLE innsynskrav
  ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone;

UPDATE innsynskrav
SET verified_at = opprettet_dato
WHERE verified IS TRUE AND verified_at IS NULL;

DROP INDEX IF EXISTS innsynskrav_verified;
