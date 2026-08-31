ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS verified_at timestamp with time zone;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'innsynskrav'
      AND column_name = 'verified'
  ) THEN
    UPDATE innsynskrav
    SET verified_at = CASE
      WHEN verified IS TRUE THEN opprettet_dato
      ELSE NULL
    END;
  END IF;
END $$;

DROP INDEX IF EXISTS innsynskrav_verified;

ALTER TABLE IF EXISTS innsynskrav
  DROP COLUMN IF EXISTS verified;
