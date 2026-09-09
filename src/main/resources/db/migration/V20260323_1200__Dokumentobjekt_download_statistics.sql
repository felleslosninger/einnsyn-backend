CREATE TABLE IF NOT EXISTS dokumentobjekt_download_stat (
  _id TEXT COLLATE "C" PRIMARY KEY
);

ALTER TABLE dokumentobjekt_download_stat
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ NOT NULL DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS dokumentobjekt__id TEXT NOT NULL,
  ADD COLUMN IF NOT EXISTS bucket_start TIMESTAMPTZ NOT NULL,
  ADD COLUMN IF NOT EXISTS download_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_download_stat_dokobj_bucket_idx
  ON dokumentobjekt_download_stat (dokumentobjekt__id, bucket_start);
CREATE INDEX IF NOT EXISTS dokumentobjekt_download_stat_bucket_start_idx
  ON dokumentobjekt_download_stat (bucket_start);
