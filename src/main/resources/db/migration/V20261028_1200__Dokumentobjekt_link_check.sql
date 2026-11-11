ALTER TABLE dokumentobjekt
  ADD COLUMN IF NOT EXISTS link_checked_at timestamp with time zone,
  ADD COLUMN IF NOT EXISTS link_failed_since timestamp with time zone;

-- Used by the link checker to find the links that were checked longest ago
CREATE INDEX IF NOT EXISTS dokumentobjekt_link_checked_at_idx
  ON dokumentobjekt (link_checked_at NULLS FIRST);

-- Used by the link checker to find failed links that should be rechecked
CREATE INDEX IF NOT EXISTS dokumentobjekt_link_failed_idx
  ON dokumentobjekt (link_checked_at) WHERE link_failed_since IS NOT NULL;
