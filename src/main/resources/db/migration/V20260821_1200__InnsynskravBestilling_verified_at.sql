ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'innsynskrav'
      AND column_name = 'oppdatert_dato'
  ) THEN
    UPDATE innsynskrav
    SET verified_at = COALESCE(oppdatert_dato, _updated, opprettet_dato)
    WHERE verified IS TRUE
      AND verified_at IS NULL;
  ELSE
    UPDATE innsynskrav
    SET verified_at = COALESCE(_updated, opprettet_dato)
    WHERE verified IS TRUE
      AND verified_at IS NULL;
  END IF;
END $$;

DROP INDEX IF EXISTS innsynskrav_verified;

ALTER TABLE IF EXISTS innsynskrav
  DROP COLUMN IF EXISTS verified;
