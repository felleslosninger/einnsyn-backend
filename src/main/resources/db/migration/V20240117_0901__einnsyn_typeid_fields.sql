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
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe__id_idx ON saksmappe (_id);
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe__external_id_idx ON saksmappe (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_system_id_idx ON saksmappe (system_id);
CREATE INDEX IF NOT EXISTS saksmappe_adm_enhet_idx ON saksmappe (administrativ_enhet__id);
CREATE INDEX IF NOT EXISTS saksmappe_journalenhet_idx ON saksmappe (journalenhet__id);
CREATE INDEX IF NOT EXISTS saksmappe__created_idx ON saksmappe (_created);
CREATE INDEX IF NOT EXISTS saksmappe__updated_idx ON saksmappe (_updated);
/* Workaround for the legacy UPDATE statements: */
CREATE INDEX IF NOT EXISTS saksmappe__created_null_idx ON saksmappe (_created) WHERE _created IS NULL;
CREATE INDEX IF NOT EXISTS saksmappe__updated_null_idx ON saksmappe (_updated) WHERE _updated IS NULL;
UPDATE saksmappe SET _created = COALESCE(publisert_dato, now()) WHERE _created IS NULL;
UPDATE saksmappe SET _updated = COALESCE(publisert_dato, now()) WHERE _updated IS NULL;
ALTER TABLE IF EXISTS saksmappe
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
UPDATE saksmappe SET _external_id = saksmappe_iri WHERE _external_id IS NULL AND saksmappe_iri IS NOT NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_trigger ON saksmappe;
CREATE TRIGGER enrich_legacy_saksmappe_trigger BEFORE INSERT OR UPDATE ON saksmappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_saksmappe();

/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS saksbehandler TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS journalpost__id_idx ON journalpost (_id);
CREATE UNIQUE INDEX IF NOT EXISTS journalpost__external_id_idx ON journalpost (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_system_id_idx ON journalpost (system_id);
CREATE INDEX IF NOT EXISTS journalpost_journalenhet_idx ON journalpost (journalenhet__id);
CREATE INDEX IF NOT EXISTS journalpost__created_idx ON journalpost (_created);
CREATE INDEX IF NOT EXISTS journalpost__updated_idx ON journalpost (_updated);
/* Workaround for the legacy UPDATE statements: */
CREATE INDEX IF NOT EXISTS journalpost__created_null_idx ON journalpost (_created) WHERE _created IS NULL;
CREATE INDEX IF NOT EXISTS journalpost__updated_null_idx ON journalpost (_updated) WHERE _updated IS NULL;
UPDATE journalpost SET _created = COALESCE(publisert_dato, now()) WHERE _created IS NULL;
UPDATE journalpost SET _updated = COALESCE(publisert_dato, now()) WHERE _updated IS NULL;
ALTER TABLE IF EXISTS journalpost
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
UPDATE journalpost SET _external_id = journalpost_iri WHERE _external_id IS NULL AND journalpost_iri IS NOT NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_trigger ON journalpost;
CREATE TRIGGER enrich_legacy_journalpost_trigger BEFORE INSERT OR UPDATE ON journalpost
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalpost();

/* Enhet */
ALTER TABLE IF EXISTS enhet
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('enhet'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1;
CREATE UNIQUE INDEX IF NOT EXISTS enhet__id_idx ON enhet (_id);
CREATE UNIQUE INDEX IF NOT EXISTS enhet__external_id_idx ON enhet (_external_id);
CREATE INDEX IF NOT EXISTS enhet__created_idx ON enhet (_created);
CREATE INDEX IF NOT EXISTS enhet__updated_idx ON enhet (_updated);
UPDATE enhet SET _created = COALESCE(opprettet_dato, now()) WHERE _created IS NULL;
UPDATE enhet SET _updated = COALESCE(oppdatert_dato, now()) WHERE _updated IS NULL;
ALTER TABLE IF EXISTS enhet
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now(),
  ALTER COLUMN opprettet_dato SET DEFAULT now(),
  ALTER COLUMN oppdatert_dato SET DEFAULT now(),
  ALTER COLUMN e_formidling SET DEFAULT false,
  ALTER COLUMN skjult SET DEFAULT false;
UPDATE enhet SET _external_id = iri WHERE _external_id IS NULL AND iri IS NOT NULL;
-- TODO: This trigger should be removed when the legacy import is killed
DROP TRIGGER IF EXISTS enrich_legacy_enhet_trigger ON enhet;
CREATE TRIGGER enrich_legacy_enhet_trigger BEFORE INSERT OR UPDATE ON enhet
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_enhet();

/* Bruker */
ALTER TABLE IF EXISTS bruker
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('user'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS language TEXT DEFAULT 'nb';
CREATE UNIQUE INDEX IF NOT EXISTS bruker__id_idx ON bruker (_id);
CREATE UNIQUE INDEX IF NOT EXISTS bruker__external_id_idx ON bruker (_external_id);
CREATE INDEX IF NOT EXISTS bruker__created_idx ON bruker (_created);
CREATE INDEX IF NOT EXISTS bruker__updated_idx ON bruker (_updated);
UPDATE bruker SET _created = COALESCE(opprettet_dato, now()) WHERE _created IS NULL;
UPDATE bruker SET _updated = COALESCE(oppdatert_dato, now()) WHERE _updated IS NULL;
ALTER TABLE IF EXISTS bruker
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();

/* Skjerming */
ALTER TABLE IF EXISTS skjerming
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('skj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but Skjerming should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS skjerming__id_idx ON skjerming (_id);
CREATE UNIQUE INDEX IF NOT EXISTS skjerming__external_id_idx ON skjerming (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS skjerming_system_id_idx ON skjerming (system_id);
CREATE INDEX IF NOT EXISTS skjerming_journalenhet_idx ON skjerming (journalenhet__id);
CREATE INDEX IF NOT EXISTS skjerming__created_idx ON skjerming (_created);
CREATE INDEX IF NOT EXISTS skjerming__updated_idx ON skjerming (_updated);

/* Korrespondansepart */
ALTER TABLE IF EXISTS korrespondansepart
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kpart'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedokument__id TEXT,
  ADD COLUMN IF NOT EXISTS moetesak__id TEXT,
  ADD COLUMN IF NOT EXISTS er_behandlingsansvarlig BOOLEAN DEFAULT FALSE,
  ALTER COLUMN journalpost_id DROP NOT NULL, /* Korrespondansepart could also be tied to moetesak / moetedokument */
  /* This is a legacy field, but Korrespondansepart should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart__id_idx ON korrespondansepart (_id);
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart__external_id_idx ON korrespondansepart (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart_system_id_idx ON korrespondansepart (system_id);
CREATE INDEX IF NOT EXISTS korrespondansepart_journalenhet_idx ON korrespondansepart (journalenhet__id);
CREATE INDEX IF NOT EXISTS korrespondansepart_moetedokument_idx ON korrespondansepart (moetedokument__id);
CREATE INDEX IF NOT EXISTS korrespondansepart_moetesak_idx ON korrespondansepart (moetesak__id);

/* Dokumentbeskrivelse */
ALTER TABLE IF EXISTS dokumentbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokbesk'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse__id_idx ON dokumentbeskrivelse (_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse__external_id_idx ON dokumentbeskrivelse (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse_system_id_idx ON dokumentbeskrivelse (system_id);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse_journalenhet_idx ON dokumentbeskrivelse (journalenhet__id);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse__created_idx ON dokumentbeskrivelse (_created);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse__updated_idx ON dokumentbeskrivelse (_updated);

/* Dokumentobjekt */
ALTER TABLE IF EXISTS dokumentobjekt
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokobj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
    /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt__id_idx ON dokumentobjekt (_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt__external_id_idx ON dokumentobjekt (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_system_id_idx ON dokumentobjekt (system_id);
CREATE INDEX IF NOT EXISTS dokumentobjekt_journalenhet_idx ON dokumentobjekt (journalenhet__id);
CREATE INDEX IF NOT EXISTS dokumentobjekt__created_idx ON dokumentobjekt (_created);
CREATE INDEX IF NOT EXISTS dokumentobjekt__updated_idx ON dokumentobjekt (_updated);

/* Innsynskrav */
ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ik'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS language TEXT,
  ADD COLUMN IF NOT EXISTS locked BOOLEAN,
  ADD COLUMN IF NOT EXISTS bruker__id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav__id_idx ON innsynskrav (_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav__external_id_idx ON innsynskrav (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_system_id_idx ON innsynskrav (system_id);
CREATE INDEX IF NOT EXISTS innsynskrav_journalenhet_idx ON innsynskrav (journalenhet__id);
CREATE INDEX IF NOT EXISTS innsynskrav__created_idx ON innsynskrav (_created);
CREATE INDEX IF NOT EXISTS innsynskrav__updated_idx ON innsynskrav (_updated);
CREATE INDEX IF NOT EXISTS innsynskrav_bruker_idx ON innsynskrav (bruker__id);
CREATE INDEX IF NOT EXISTS innsynskrav_verified ON innsynskrav(id, verified);
UPDATE innsynskrav SET bruker__id = bruker._id FROM bruker WHERE innsynskrav.bruker_iri = bruker._external_id;
/* TODO: Trigger for incoming data? bruker__id */

/* InnsynskravDel */
ALTER TABLE IF EXISTS innsynskrav_del
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ikd'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS journalpost__id TEXT,
  ADD COLUMN IF NOT EXISTS enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS sent TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS retry_timestamp TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1;
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del__id_idx ON innsynskrav_del (_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del__external_id_idx ON innsynskrav_del (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del_system_id_idx ON innsynskrav_del (system_id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_journalenhet_idx ON innsynskrav_del (journalenhet__id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_journalpost_idx ON innsynskrav_del (journalpost__id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_enhet_idx ON innsynskrav_del (enhet__id);
CREATE INDEX IF NOT EXISTS innsynskrav_del__created_idx ON innsynskrav_del (_created);
CREATE INDEX IF NOT EXISTS innsynskrav_del__updated_idx ON innsynskrav_del (_updated);
CREATE INDEX IF NOT EXISTS innsynskrav_del_retries ON innsynskrav_del(sent, innsynskrav_id, retry_timestamp, retry_count);
/* TODO: Lookup journalpost__id, enhet__id from old entries, add not null after */
