ALTER TABLE IF EXISTS lagret_sok
  ADD COLUMN IF NOT EXISTS search_parameters TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ,
  ALTER COLUMN sporring DROP NOT NULL;
CREATE INDEX IF NOT EXISTS lagret_soek_last_indexed_idx ON lagret_sok (last_indexed);