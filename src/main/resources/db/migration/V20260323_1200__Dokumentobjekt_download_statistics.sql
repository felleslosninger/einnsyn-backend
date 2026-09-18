CREATE TABLE IF NOT EXISTS dokumentobjekt_download_stat (
  _id TEXT COLLATE "C" PRIMARY KEY,
  _external_id TEXT,
  _created TIMESTAMPTZ NOT NULL DEFAULT now(),
  _updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  _accessible_after TIMESTAMPTZ,
  lock_version BIGINT NOT NULL DEFAULT 0,
  dokumentobjekt__id TEXT NOT NULL,
  bucket_start TIMESTAMPTZ NOT NULL,
  download_count INT NOT NULL DEFAULT 0,
  -- Attribution captured when the bucket is created. Plain ids, deliberately without foreign keys:
  -- buckets outlive the Dokumentobjekt, its parent and even the Enhet, and must keep recording what
  -- the attribution was at the time.
  parent__id TEXT,
  enhet__id TEXT,
  last_indexed TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS dokumentobjekt_download_stat_enhet_idx
  ON dokumentobjekt_download_stat (enhet__id);

CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_download_stat_dokobj_bucket_idx
  ON dokumentobjekt_download_stat (dokumentobjekt__id, bucket_start);

-- Indexes for the reindex scheduler's stale-row detection, matching the other indexable tables
CREATE INDEX IF NOT EXISTS dokumentobjekt_download_stat_last_indexed_idx
  ON dokumentobjekt_download_stat (last_indexed);
CREATE INDEX IF NOT EXISTS dokumentobjekt_download_stat_last_indexed_stale_partial_idx
  ON dokumentobjekt_download_stat (last_indexed, _updated)
  WHERE last_indexed < _updated;
CREATE INDEX IF NOT EXISTS dokumentobjekt_download_stat_last_indexed_null_partial_idx
  ON dokumentobjekt_download_stat (last_indexed)
  WHERE last_indexed IS NULL;
