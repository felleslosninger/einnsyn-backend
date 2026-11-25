-- Used by the link checker to find the links that were checked longest ago.
-- In prod, create this index manually with CREATE INDEX CONCURRENTLY before deploying, so the
-- table isn't locked for writes while it is built. This migration then does nothing.
CREATE INDEX IF NOT EXISTS dokumentobjekt_link_checked_at_idx
  ON dokumentobjekt (link_checked_at NULLS FIRST);
