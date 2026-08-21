ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS verifisert_dato timestamp with time zone;
