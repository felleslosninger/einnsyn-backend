-- Only adds the columns, so the exclusive lock on the table is released right away. The index is
-- added in a separate migration, so the lock isn't held while it is built.
ALTER TABLE dokumentobjekt
  ADD COLUMN IF NOT EXISTS link_checked_at timestamp with time zone,
  ADD COLUMN IF NOT EXISTS link_failed_since timestamp with time zone;
