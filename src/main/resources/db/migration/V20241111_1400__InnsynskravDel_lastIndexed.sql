ALTER TABLE IF EXISTS innsynskrav_del
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS innsynskrav_del_last_indexed_idx ON innsynskrav_del (last_indexed);