-- ID generator
create or replace function einnsyn_id(prefix text)
returns text
as $$
declare
  uuid uuid;
  suffix text;
begin
  if (prefix is null) or not (prefix ~ '^[a-z]{0,63}$') then
    raise exception 'prefix must match the regular expression [a-z]{0,63}';
  end if;
  uuid = uuid_generate_v7();
  suffix = base32_encode(uuid);
  return (prefix || '_' || suffix);
end
$$
language plpgsql
volatile;


-- Update data coming from legacy import
CREATE OR REPLACE FUNCTION enrich_legacy_journalpost()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.journalpost_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION enrich_legacy_saksmappe()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.saksmappe_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION enrich_legacy_enhet()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;



/* Saksmappe */
ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('sm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP,
  ADD COLUMN IF NOT EXISTS slug TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet_id UUID,
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID;
UPDATE saksmappe SET _created = publisert_dato WHERE _created IS NULL;
UPDATE saksmappe SET _created = now() WHERE _created IS NULL;
UPDATE saksmappe SET _updated = publisert_dato WHERE _updated IS NULL;
UPDATE saksmappe SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS saksmappe
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_id_idx ON saksmappe (_id);
UPDATE saksmappe SET _external_id = saksmappe_iri WHERE _external_id IS NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_trigger ON saksmappe;
CREATE TRIGGER enrich_legacy_saksmappe_trigger BEFORE INSERT OR UPDATE ON saksmappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_saksmappe();
  -- Add slug. This should be done in Java when the old import is killed.
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_administrativ_enhet_id_slug_idx
  ON saksmappe (administrativ_enhet_id, slug) WHERE slug IS NOT NULL;
DROP TRIGGER IF EXISTS saksmappe_slug_trigger ON saksmappe;
CREATE TRIGGER saksmappe_slug_trigger AFTER INSERT ON saksmappe
  FOR EACH ROW EXECUTE PROCEDURE update_slug('saksaar', 'sakssekvensnummer', 'offentlig_tittel');

/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP,
  ADD COLUMN IF NOT EXISTS slug TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet_id UUID,
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID,
  ADD COLUMN IF NOT EXISTS saksbehandler TEXT;
UPDATE journalpost SET _created = publisert_dato WHERE _created IS NULL;
UPDATE journalpost SET _created = now() WHERE _created IS NULL;
UPDATE journalpost SET _updated = publisert_dato WHERE _updated IS NULL;
UPDATE journalpost SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS journalpost
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_id_idx ON journalpost (_id);
UPDATE journalpost SET _external_id = journalpost_iri WHERE _external_id IS NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_trigger ON journalpost;
CREATE TRIGGER enrich_legacy_journalpost_trigger BEFORE INSERT OR UPDATE ON journalpost
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalpost();
-- Add slug. This should be done in Java when the old import is killed.
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_saksmappe_id_slug_idx
  ON journalpost (saksmappe_id, slug) WHERE slug IS NOT NULL;
DROP TRIGGER IF EXISTS journalpost_slug_trigger ON journalpost;
CREATE TRIGGER journalpost_slug_trigger AFTER INSERT ON journalpost
  FOR EACH ROW EXECUTE PROCEDURE update_slug('offentlig_tittel');

/* Enhet */
ALTER TABLE IF EXISTS enhet
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('enhet'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP,
  ADD COLUMN IF NOT EXISTS slug TEXT,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL,
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID;
UPDATE enhet SET _created = opprettet_dato WHERE _created IS NULL;
UPDATE enhet SET _created = now() WHERE _created IS NULL;
UPDATE enhet SET _updated = oppdatert_dato WHERE _updated IS NULL;
UPDATE enhet SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS enhet
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS enhet_id_idx ON enhet (_id);
UPDATE enhet SET _external_id = iri WHERE _external_id IS NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_enhet_trigger ON enhet;
CREATE TRIGGER enrich_legacy_enhet_trigger BEFORE INSERT OR UPDATE ON enhet
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_enhet();
-- Add slug. This should be done in Java when the old import is killed.
CREATE UNIQUE INDEX IF NOT EXISTS enhet_parent_id_slug_idx
  ON enhet (parent_id, slug) WHERE slug IS NOT NULL;
DROP TRIGGER IF EXISTS enhet_slug_trigger ON enhet;
CREATE TRIGGER enhet_slug_trigger AFTER INSERT ON enhet
  FOR EACH ROW EXECUTE PROCEDURE update_slug('navn');

/* Skjerming */
ALTER TABLE IF EXISTS skjerming
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('skj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS skjerming_id_idx ON skjerming (_id);

/* Korrespondansepart */
ALTER TABLE IF EXISTS korrespondansepart
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kpart'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID,
  ADD COLUMN IF NOT EXISTS er_behandlingsansvarlig BOOLEAN DEFAULT FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart_id_idx ON korrespondansepart (_id);

/* Dokumentbeskrivelse */
ALTER TABLE IF EXISTS dokumentbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokbesk'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse_id_idx ON dokumentbeskrivelse (_id);

/* Dokumentobjekt */
ALTER TABLE IF EXISTS dokumentobjekt
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokobj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_id_idx ON dokumentobjekt (_id);

/* Innsynskrav */
ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ik'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL,
  ADD COLUMN IF NOT EXISTS language TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_id_idx ON innsynskrav (_id);
CREATE INDEX IF NOT EXISTS innsynskrav_verified ON innsynskrav(id, verified);

/* InnsynskravDel */
ALTER TABLE IF EXISTS innsynskrav_del
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ikd'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS journalenhet_id UUID,
  ADD COLUMN IF NOT EXISTS journalpost_id BIGINT NOT NULL,
  ADD COLUMN IF NOT EXISTS enhet_id UUID NOT NULL,
  ADD COLUMN IF NOT EXISTS sent TIMESTAMP,
  ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS retry_timestamp TIMESTAMP,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del_id_idx ON innsynskrav_del (_id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_retries ON innsynskrav_del(sent, innsynskrav_id, retry_timestamp, retry_count);
