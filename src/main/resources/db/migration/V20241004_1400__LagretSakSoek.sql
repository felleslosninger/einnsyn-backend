ALTER TABLE IF EXISTS lagret_sak
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('lsak'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS bruker__id TEXT,
  ADD COLUMN IF NOT EXISTS saksmappe__id TEXT,
  ADD COLUMN IF NOT EXISTS moetemappe__id TEXT,
  ADD COLUMN IF NOT EXISTS enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS hit_count INT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1;
SELECT add_foreign_key_if_not_exists('lagret_sak', 'bruker__id', 'bruker', '_id');
SELECT add_foreign_key_if_not_exists('lagret_sak', 'saksmappe__id', 'saksmappe', '_id');
SELECT add_foreign_key_if_not_exists('lagret_sak', 'moetemappe__id', 'møtemappe', '_id');
SELECT add_foreign_key_if_not_exists('lagret_sak', 'enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS lagret_sak__id_idx ON lagret_sak (_id);
CREATE UNIQUE INDEX IF NOT EXISTS lagret_sak__external_id_idx ON lagret_sak (_external_id);
CREATE INDEX IF NOT EXISTS lagret_sak_bruker__id_idx ON lagret_sak (bruker__id);
CREATE INDEX IF NOT EXISTS lagret_sak_saksmappe__id_idx ON lagret_sak (saksmappe__id);
CREATE INDEX IF NOT EXISTS lagret_sak_moetemappe__id_idx ON lagret_sak (moetemappe__id);
CREATE INDEX IF NOT EXISTS lagret_sak_enhet__id_idx ON lagret_sak (enhet__id);
CREATE INDEX IF NOT EXISTS lagret_sak__created_idx ON lagret_sak (_created);
CREATE INDEX IF NOT EXISTS lagret_sak__updated_idx ON lagret_sak (_updated);

CREATE OR REPLACE FUNCTION enrich_legacy_lagret_sak()
RETURNS TRIGGER AS $$
BEGIN
  -- Set bruker__id
  IF NEW.bruker__id IS NULL AND NEW.bruker_id IS NOT NULL THEN
    SELECT _id INTO NEW.bruker__id FROM bruker WHERE id = NEW.bruker_id;
  END IF;
  -- Find saksmappe from sak_id
  IF NEW.saksmappe__id IS NULL AND NEW.sak_id IS NOT NULL THEN
    SELECT _id INTO NEW.saksmappe__id FROM saksmappe WHERE saksmappe_iri = NEW.sak_id;
  END IF;
  -- Find moetemappe from sak_id
  IF NEW.moetemappe__id IS NULL AND NEW.sak_id IS NOT NULL THEN
    SELECT _id INTO NEW.moetemappe__id FROM møtemappe WHERE møtemappe_iri = NEW.sak_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_lagret_sak_trigger ON lagret_sak;
CREATE TRIGGER enrich_legacy_lagret_sak_trigger BEFORE INSERT OR UPDATE ON lagret_sak
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_lagret_sak();




ALTER TABLE IF EXISTS lagret_sok
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('lsoek'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS bruker__id TEXT,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1;
SELECT add_foreign_key_if_not_exists('lagret_sok', 'bruker__id', 'bruker', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS lagret_sok__id_idx ON lagret_sok (_id);
CREATE UNIQUE INDEX IF NOT EXISTS lagret_sok__external_id_idx ON lagret_sok (_external_id);
CREATE INDEX IF NOT EXISTS lagret_sok__created_idx ON lagret_sok (_created);
CREATE INDEX IF NOT EXISTS lagret_sok__updated_idx ON lagret_sok (_updated);

CREATE OR REPLACE FUNCTION enrich_legacy_lagret_sok()
RETURNS TRIGGER AS $$
BEGIN
  -- Set bruker__id
  IF NEW.bruker__id IS NULL AND NEW.bruker_id IS NOT NULL THEN
    SELECT _id INTO NEW.bruker__id FROM bruker WHERE id = NEW.bruker_id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_lagret_sok_trigger ON lagret_sok;
CREATE TRIGGER enrich_legacy_lagret_sok_trigger BEFORE INSERT OR UPDATE ON lagret_sok
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_lagret_sok();


-- LagretSoekHit
CREATE TABLE IF NOT EXISTS lagret_soek_hit(
  _id TEXT DEFAULT einnsyn_id('lagretsoekhit')
);
ALTER TABLE lagret_soek_hit
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('lagretsoekhit'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS lagret_soek__id TEXT NOT NULL,
  ADD COLUMN IF NOT EXISTS saksmappe__id TEXT,
  ADD COLUMN IF NOT EXISTS journalpost__id TEXT,
  ADD COLUMN IF NOT EXISTS moetemappe__id TEXT,
  ADD COLUMN IF NOT EXISTS moetesak__id TEXT,
  DROP CONSTRAINT IF EXISTS fk_lagret_soek,
  ADD CONSTRAINT fk_lagret_soek FOREIGN KEY (lagret_soek__id) REFERENCES lagret_sok(_id) ON DELETE CASCADE,
  DROP CONSTRAINT IF EXISTS fk_saksmappe,
  ADD CONSTRAINT fk_saksmappe FOREIGN KEY (saksmappe__id) REFERENCES saksmappe(_id) ON DELETE CASCADE,
  DROP CONSTRAINT IF EXISTS fk_journalpost,
  ADD CONSTRAINT fk_journalpost FOREIGN KEY (journalpost__id) REFERENCES journalpost(_id) ON DELETE CASCADE,
  DROP CONSTRAINT IF EXISTS fk_moetemappe,
  ADD CONSTRAINT fk_moetemappe FOREIGN KEY (moetemappe__id) REFERENCES møtemappe(_id) ON DELETE CASCADE,
  DROP CONSTRAINT IF EXISTS fk_moetesak,
  ADD CONSTRAINT fk_moetesak FOREIGN KEY (moetesak__id) REFERENCES møtesaksregistrering(_id) ON DELETE CASCADE;
  