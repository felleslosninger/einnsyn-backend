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


-- ADD CONSTRAINT IF NOT EXISTS does not exist in PostgreSQL
CREATE OR REPLACE FUNCTION add_foreign_key_if_not_exists(
    p_table_name TEXT,
    p_column_name TEXT,
    p_reference_table TEXT,
    p_reference_column TEXT
)
RETURNS VOID AS $$
DECLARE
    v_constraint_name TEXT;
BEGIN
    -- Generate the constraint name by concatenating the table name and column name
    v_constraint_name := format('fk_%s_%s', p_table_name, p_column_name);

    -- Check if the constraint already exists
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = v_constraint_name
    ) THEN
        -- Construct and execute the ALTER TABLE statement
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I(%I) ON DELETE SET NULL;',
            p_table_name, v_constraint_name, p_column_name, p_reference_table, p_reference_column
        );
    END IF;
END;
$$ LANGUAGE plpgsql;


/*
 * A trigger that looks up journalenhet's _id based on the legacy virksomhet_iri
 * field.
 */
CREATE OR REPLACE FUNCTION enrich_legacy_journalenhet()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.journalenhet__id IS NULL AND NEW.virksomhet_iri IS NOT NULL THEN
    SELECT _id INTO NEW.journalenhet__id FROM enhet WHERE iri = NEW.virksomhet_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/* Enhet */
ALTER TABLE IF EXISTS enhet
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('enh'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ALTER COLUMN opprettet_dato SET DEFAULT now(),
  ALTER COLUMN oppdatert_dato SET DEFAULT now(),
  ALTER COLUMN e_formidling SET DEFAULT false,
  ALTER COLUMN skjult SET DEFAULT false;
CREATE UNIQUE INDEX IF NOT EXISTS enhet__id_idx ON enhet (_id);
CREATE UNIQUE INDEX IF NOT EXISTS enhet__external_id_idx ON enhet (_external_id);
CREATE INDEX IF NOT EXISTS enhet__created_idx ON enhet (_created);
CREATE INDEX IF NOT EXISTS enhet__updated_idx ON enhet (_updated);
UPDATE enhet SET _external_id = iri WHERE _external_id IS NULL;
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_enhet()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.iri IS NOT NULL AND NEW.iri != NEW._id THEN
    NEW._external_id := NEW.iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_enhet_trigger ON enhet;
CREATE TRIGGER enrich_legacy_enhet_trigger BEFORE INSERT OR UPDATE ON enhet
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_enhet();


/* Arkiv */
CREATE TABLE IF NOT EXISTS arkiv(
  _id TEXT DEFAULT einnsyn_id('ark')
);
ALTER TABLE IF EXISTS arkiv
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ark'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('arkiv', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS arkiv_id_idx ON arkiv (_id);
CREATE UNIQUE INDEX IF NOT EXISTS arkiv_external_id_idx ON arkiv (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS arkiv_system_id_idx ON arkiv (system_id);
CREATE INDEX IF NOT EXISTS arkiv_system_id_nonunique_idx ON arkiv (system_id);
CREATE INDEX IF NOT EXISTS arkiv_created_idx ON arkiv (_created);
CREATE INDEX IF NOT EXISTS arkiv_updated_idx ON arkiv (_updated);
CREATE INDEX IF NOT EXISTS arkiv_journalenhet__id ON arkiv(journalenhet__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_arkiv()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.arkiv_iri IS NOT NULL AND NEW.arkiv_iri != NEW._id THEN
    NEW._external_id = NEW.arkiv_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_arkiv_trigger ON arkiv;
CREATE TRIGGER enrich_legacy_arkiv_trigger BEFORE INSERT OR UPDATE ON arkiv
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_arkiv();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_arkiv_journalenhet_trigger ON arkiv;
CREATE TRIGGER enrich_legacy_arkiv_journalenhet_trigger BEFORE INSERT OR UPDATE ON arkiv
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Arkivdel */
CREATE TABLE IF NOT EXISTS arkivdel(
  _id TEXT DEFAULT einnsyn_id('arkd')
);
ALTER TABLE IF EXISTS arkivdel
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('arkd'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('arkivdel', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS arkivdel_id_idx ON arkivdel (_id);
CREATE UNIQUE INDEX IF NOT EXISTS arkivdel_external_id_idx ON arkivdel (_external_id);
/*CREATE UNIQUE INDEX IF NOT EXISTS arkivdel_system_id_idx ON arkivdel (system_id);*/
CREATE INDEX IF NOT EXISTS arkivdel_system_id_nonunique_idx ON arkivdel (system_id);
CREATE INDEX IF NOT EXISTS arkivdel_created_idx ON arkivdel (_created);
CREATE INDEX IF NOT EXISTS arkivdel_updated_idx ON arkivdel (_updated);
CREATE INDEX IF NOT EXISTS arkivdel_journalenhet__id ON arkivdel(journalenhet__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_arkivdel()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.arkivdel_iri IS NOT NULL AND NEW.arkivdel_iri != NEW._id THEN
    NEW._external_id = NEW.arkivdel_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_arkivdel_trigger ON arkivdel;
CREATE TRIGGER enrich_legacy_arkivdel_trigger BEFORE INSERT OR UPDATE ON arkivdel
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_arkivdel();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_arkivdel_journalenhet_trigger ON arkivdel;
CREATE TRIGGER enrich_legacy_arkivdel_journalenhet_trigger BEFORE INSERT OR UPDATE ON arkivdel
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Klassifikasjonssystem */
CREATE TABLE IF NOT EXISTS klassifikasjonssystem(
  _id TEXT DEFAULT einnsyn_id('ksys')
);
ALTER TABLE IF EXISTS klassifikasjonssystem
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ksys'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS lock_version INT DEFAULT 0,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT,
  ADD COLUMN IF NOT EXISTS arkivdel__id TEXT,
  ADD COLUMN IF NOT EXISTS tittel TEXT;
SELECT add_foreign_key_if_not_exists('klassifikasjonssystem', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('klassifikasjonssystem', 'arkivdel__id', 'arkivdel', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_id_idx ON klassifikasjonssystem (_id);
CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_external_id_idx ON klassifikasjonssystem (_external_id);
/*CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_system_id_idx ON klassifikasjonssystem (system_id);*/
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_system_id_nonunique_idx ON klassifikasjonssystem (system_id);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_created_idx ON klassifikasjonssystem (_created);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_updated_idx ON klassifikasjonssystem (_updated);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_journalenhet__id ON klassifikasjonssystem(journalenhet__id);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_arkivdel ON klassifikasjonssystem(arkivdel__id);


/* Klasse */
CREATE TABLE IF NOT EXISTS klasse(
  _id TEXT DEFAULT einnsyn_id('kla')
);
ALTER TABLE IF EXISTS klasse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kla'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT,
  ADD COLUMN IF NOT EXISTS klassifikasjonssystem__id TEXT;
SELECT add_foreign_key_if_not_exists('klasse', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('klasse', 'klassifikasjonssystem__id', 'klassifikasjonssystem', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS klasse_id_idx ON klasse (_id);
CREATE UNIQUE INDEX IF NOT EXISTS klasse_external_id_idx ON klasse (_external_id);
/*CREATE UNIQUE INDEX IF NOT EXISTS klasse_system_id_idx ON klasse (system_id);*/
CREATE INDEX IF NOT EXISTS klasse_system_id_nonunique_idx ON klasse (system_id);
CREATE INDEX IF NOT EXISTS klasse_created_idx ON klasse (_created);
CREATE INDEX IF NOT EXISTS klasse_updated_idx ON klasse (_updated);
CREATE INDEX IF NOT EXISTS klasse_klassifikasjonssystem__id ON klasse(klassifikasjonssystem__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_klasse()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.klasse_iri IS NOT NULL AND NEW.klasse_iri != NEW._id THEN
    NEW._external_id = NEW.klasse_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_klasse_trigger ON klasse;
CREATE TRIGGER enrich_legacy_klasse_trigger BEFORE INSERT OR UPDATE ON klasse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_klasse();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_klasse_journalenhet_trigger ON klasse;
CREATE TRIGGER enrich_legacy_klasse_journalenhet_trigger BEFORE INSERT OR UPDATE ON klasse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Saksmappe */
ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('sm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
SELECT add_foreign_key_if_not_exists('saksmappe', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('saksmappe', 'administrativ_enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe__id_idx ON saksmappe (_id);
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe__external_id_idx ON saksmappe (_external_id);
-- CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_system_id_idx ON saksmappe (system_id);
CREATE INDEX IF NOT EXISTS saksmappe_system_id_nonunique_idx ON saksmappe (system_id);
CREATE INDEX IF NOT EXISTS saksmappe_adm_enhet_idx ON saksmappe (administrativ_enhet__id);
CREATE INDEX IF NOT EXISTS saksmappe_journalenhet_idx ON saksmappe (journalenhet__id);
CREATE INDEX IF NOT EXISTS saksmappe__created_idx ON saksmappe (_created);
CREATE INDEX IF NOT EXISTS saksmappe__updated_idx ON saksmappe (_updated);
CREATE INDEX IF NOT EXISTS saksmappe__created_null_idx ON saksmappe (_created) WHERE _created IS NULL;
CREATE INDEX IF NOT EXISTS saksmappe__updated_null_idx ON saksmappe (_updated) WHERE _updated IS NULL;
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_saksmappe()
RETURNS TRIGGER AS $$
BEGIN
  -- Set _external_id to saksmappe_iri for old import
  IF NEW._external_id IS NULL AND NEW.saksmappe_iri IS NOT NULL AND NEW.saksmappe_iri != NEW._id THEN
    NEW._external_id := NEW.saksmappe_iri;
  END IF;
  -- Look up adminiistrativ_enhet__id for old import
  IF TG_OP = 'UPDATE' AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper THEN
    SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_trigger ON saksmappe;
CREATE TRIGGER enrich_legacy_saksmappe_trigger BEFORE INSERT OR UPDATE ON saksmappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_saksmappe();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_journalenhet_trigger ON saksmappe;
CREATE TRIGGER enrich_legacy_saksmappe_journalenhet_trigger BEFORE INSERT OR UPDATE ON saksmappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ default now(),
  ADD COLUMN IF NOT EXISTS administrativ_enhet TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS saksbehandler TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
SELECT add_foreign_key_if_not_exists('journalpost', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('journalpost', 'administrativ_enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS journalpost__id_idx ON journalpost (_id);
CREATE UNIQUE INDEX IF NOT EXISTS journalpost__external_id_idx ON journalpost (_external_id);
-- CREATE UNIQUE INDEX IF NOT EXISTS journalpost_system_id_idx ON journalpost (system_id);
CREATE INDEX IF NOT EXISTS journalpost_system_id_nonunique_idx ON journalpost (system_id);
CREATE INDEX IF NOT EXISTS journalpost_journalenhet_idx ON journalpost (journalenhet__id);
CREATE INDEX IF NOT EXISTS journalpost__created_idx ON journalpost (_created);
CREATE INDEX IF NOT EXISTS journalpost__updated_idx ON journalpost (_updated);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_journalpost()
RETURNS TRIGGER AS $$
BEGIN
  -- Set _external_id to journalpost_iri for old import
  IF NEW._external_id IS NULL AND NEW.journalpost_iri IS NOT NULL AND NEW.journalpost_iri != NEW._id THEN
    NEW._external_id := NEW.journalpost_iri;
  END IF;
  -- Set saksmappe_iri for new import
  IF NEW.saksmappe_iri IS NULL AND NEW.saksmappe_id IS NOT NULL THEN
    SELECT _external_id INTO NEW.saksmappe_iri FROM saksmappe WHERE saksmappe_id = NEW.saksmappe_id;
  END IF;
  -- Set administrativ_enhet__id from arkivskaper for old import
  IF (NEW.administrativ_enhet__id IS NULL AND NEW.arkivskaper IS NOT NULL)
  OR (TG_OP = 'UPDATE'
    AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper
    AND NEW.administrativ_enhet__id IS NOT DISTINCT FROM OLD.administrativ_enhet__id
  ) THEN
    SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_trigger ON journalpost;
CREATE TRIGGER enrich_legacy_journalpost_trigger BEFORE INSERT OR UPDATE ON journalpost
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalpost();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_journalenhet_trigger ON journalpost;
CREATE TRIGGER enrich_legacy_journalpost_journalenhet_trigger BEFORE INSERT OR UPDATE ON journalpost
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Skjerming */
ALTER TABLE IF EXISTS skjerming
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('skj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  -- This is a legacy field, but Skjerming should inherit from ArkivBase:
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('skjerming', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS skjerming__id_idx ON skjerming (_id);
CREATE UNIQUE INDEX IF NOT EXISTS skjerming__external_id_idx ON skjerming (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS skjerming_system_id_idx ON skjerming (system_id);
CREATE INDEX IF NOT EXISTS skjerming_journalenhet_idx ON skjerming (journalenhet__id);
CREATE INDEX IF NOT EXISTS skjerming__created_idx ON skjerming (_created);
CREATE INDEX IF NOT EXISTS skjerming__updated_idx ON skjerming (_updated);
DROP INDEX IF EXISTS skjerming_skjerminghjemmel_tilgangsrestr_idx;
CREATE UNIQUE INDEX IF NOT EXISTS skjerming_hjemmel_tilgangsrestr_journalenhet_idx ON skjerming (skjermingshjemmel, tilgangsrestriksjon, journalenhet__id);
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_skjerming_journalenhet_trigger ON skjerming;
CREATE TRIGGER enrich_legacy_skjerming_journalenhet_trigger BEFORE INSERT OR UPDATE ON skjerming
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Moetesaksbeskrivelse */
CREATE TABLE IF NOT EXISTS moetesaksbeskrivelse(
  _id TEXT DEFAULT einnsyn_id('msb')
);
ALTER TABLE IF EXISTS moetesaksbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('msb'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS tekst_innhold TEXT,
  ADD COLUMN IF NOT EXISTS tekst_format TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('moetesaksbeskrivelse', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_id_idx ON moetesaksbeskrivelse (_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_external_id_idx ON moetesaksbeskrivelse (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_system_id_idx ON moetesaksbeskrivelse (system_id);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_created_idx ON moetesaksbeskrivelse (_created);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_updated_idx ON moetesaksbeskrivelse (_updated);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_journalenhet__id ON moetesaksbeskrivelse(journalenhet__id);
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksbeskrivelse_journalenhet_trigger ON moetesaksbeskrivelse;
CREATE TRIGGER enrich_legacy_moetesaksbeskrivelse_journalenhet_trigger BEFORE INSERT OR UPDATE ON moetesaksbeskrivelse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Møtedokumentregistrering */
CREATE TABLE IF NOT EXISTS møtedokumentregistrering(
  _id TEXT DEFAULT einnsyn_id('mdok')
);
ALTER TABLE IF EXISTS møtedokumentregistrering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('mdok'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ALTER COLUMN møtemappe_id DROP NOT NULL, /* Eases insertion from parent møtemappe */
  /* This is a legacy field, but the object should inherit from Registrering: */
  ADD COLUMN IF NOT EXISTS arkivskaper TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('møtedokumentregistrering', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('møtedokumentregistrering', 'administrativ_enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering_id_idx ON møtedokumentregistrering (_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering__external_id_idx ON møtedokumentregistrering (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering_system_id_idx ON møtedokumentregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_system_id_nonunique_idx ON møtedokumentregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_created_idx ON møtedokumentregistrering (_created);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_updated_idx ON møtedokumentregistrering (_updated);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_journalenhet__id ON møtedokumentregistrering(journalenhet__id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_administrativ_enhet__id ON møtedokumentregistrering(administrativ_enhet__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_moetedokumentregistrering()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.møtedokumentregistrering_iri IS NOT NULL AND NEW.møtedokumentregistrering_iri != NEW._id THEN
    NEW._external_id := NEW.møtedokumentregistrering_iri;
  END IF;
  -- Look up adminiistrativ_enhet__id for old import
  IF TG_OP = 'UPDATE' AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper THEN
    SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_moetedokumentregistrering_trigger ON møtedokumentregistrering;
CREATE TRIGGER enrich_legacy_moetedokumentregistrering_trigger BEFORE INSERT OR UPDATE ON møtedokumentregistrering
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetedokumentregistrering();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_moetedokumentregistrering_journalenhet_trigger ON møtedokumentregistrering;
CREATE TRIGGER enrich_legacy_moetedokumentregistrering_journalenhet_trigger BEFORE INSERT OR UPDATE ON møtedokumentregistrering
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Korrespondansepart */
ALTER TABLE IF EXISTS korrespondansepart
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedokument__id TEXT,
  ADD COLUMN IF NOT EXISTS moetesak__id TEXT,
  ADD COLUMN IF NOT EXISTS er_behandlingsansvarlig BOOLEAN DEFAULT FALSE,
  ALTER COLUMN journalpost_id DROP NOT NULL, -- Korrespondansepart could also be tied to moetesak / moetedokument
  -- This is a legacy field, but Korrespondansepart should inherit from ArkivBase:
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('korrespondansepart', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('korrespondansepart', 'moetesak__id', 'moetesaksbeskrivelse', '_id');
SELECT add_foreign_key_if_not_exists('korrespondansepart', 'moetedokument__id', 'møtedokumentregistrering', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart__id_idx ON korrespondansepart(_id);
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart__external_id_idx ON korrespondansepart(_external_id);
-- CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart_system_id_idx ON korrespondansepart(system_id);
CREATE INDEX IF NOT EXISTS korrespondansepart_system_id_nonunique_idx ON korrespondansepart(system_id);
CREATE INDEX IF NOT EXISTS korrespondansepart_journalenhet_idx ON korrespondansepart(journalenhet__id);
CREATE INDEX IF NOT EXISTS korrespondansepart_moetedokument_idx ON korrespondansepart(moetedokument__id);
CREATE INDEX IF NOT EXISTS korrespondansepart_moetesak_idx ON korrespondansepart(moetesak__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_korrespondansepart()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.korrespondansepart_iri IS NOT NULL AND NEW.korrespondansepart_iri != NEW._id THEN
    NEW._external_id := NEW.korrespondansepart_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_korrespondansepart_trigger ON korrespondansepart;
CREATE TRIGGER enrich_legacy_korrespondansepart_trigger BEFORE INSERT OR UPDATE ON korrespondansepart
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_korrespondansepart();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_korrespondansepart_journalenhet_trigger ON korrespondansepart;
CREATE TRIGGER enrich_legacy_korrespondansepart_journalenhet_trigger BEFORE INSERT OR UPDATE ON korrespondansepart
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Dokumentbeskrivelse */
ALTER TABLE IF EXISTS dokumentbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('db'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('dokumentbeskrivelse', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse__id_idx ON dokumentbeskrivelse (_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse__external_id_idx ON dokumentbeskrivelse(_external_id);
/*CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse_system_id_idx ON dokumentbeskrivelse(system_id);*/
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse_system_id_nonunique_idx ON dokumentbeskrivelse(system_id);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse_journalenhet_idx ON dokumentbeskrivelse(journalenhet__id);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse__created_idx ON dokumentbeskrivelse(_created);
CREATE INDEX IF NOT EXISTS dokumentbeskrivelse__updated_idx ON dokumentbeskrivelse(_updated);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_dokumentbeskrivelse()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.dokumentbeskrivelse_iri IS NOT NULL AND NEW.dokumentbeskrivelse_iri != NEW._id THEN
    NEW._external_id := NEW.dokumentbeskrivelse_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_dokumentbeskrivelse_trigger ON dokumentbeskrivelse;
CREATE TRIGGER enrich_legacy_dokumentbeskrivelse_trigger BEFORE INSERT OR UPDATE ON dokumentbeskrivelse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_dokumentbeskrivelse();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_dokumentbeskrivelse_journalenhet_trigger ON dokumentbeskrivelse;
CREATE TRIGGER enrich_legacy_dokumentbeskrivelse_journalenhet_trigger BEFORE INSERT OR UPDATE ON dokumentbeskrivelse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Dokumentobjekt */
ALTER TABLE IF EXISTS dokumentobjekt
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('do'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  -- This is a legacy field, but the object should inherit from ArkivBase:
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('dokumentobjekt', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt__id_idx ON dokumentobjekt(_id);
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt__external_id_idx ON dokumentobjekt(_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_system_id_idx ON dokumentobjekt(system_id);
CREATE INDEX IF NOT EXISTS dokumentobjekt_system_id_nonunique_idx ON dokumentobjekt(system_id);
CREATE INDEX IF NOT EXISTS dokumentobjekt_journalenhet_idx ON dokumentobjekt(journalenhet__id);
CREATE INDEX IF NOT EXISTS dokumentobjekt__created_idx ON dokumentobjekt(_created);
CREATE INDEX IF NOT EXISTS dokumentobjekt__updated_idx ON dokumentobjekt(_updated);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_dokumentobjekt()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.dokumentobjekt_iri IS NOT NULL AND NEW.dokumentobjekt_iri != NEW._id THEN
    NEW._external_id := NEW.dokumentobjekt_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_dokumentobjekt_trigger ON dokumentobjekt;
CREATE TRIGGER enrich_legacy_dokumentobjekt_trigger BEFORE INSERT OR UPDATE ON dokumentobjekt
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_dokumentobjekt();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_dokumentobjekt_journalenhet_trigger ON dokumentobjekt;
CREATE TRIGGER enrich_legacy_dokumentobjekt_journalenhet_trigger BEFORE INSERT OR UPDATE ON dokumentobjekt
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Moetemappe */
CREATE TABLE IF NOT EXISTS møtemappe(
  _id TEXT DEFAULT einnsyn_id('mm')
);
ALTER TABLE IF EXISTS møtemappe
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('mm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS utvalg__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS referanse_forrige_moete__id TEXT,
  ADD COLUMN IF NOT EXISTS referanse_neste_moete__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetemappe_id_idx ON møtemappe(_id);
SELECT add_foreign_key_if_not_exists('møtemappe', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('møtemappe', 'utvalg__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('møtemappe', 'referanse_forrige_moete__id', 'møtemappe', '_id');
SELECT add_foreign_key_if_not_exists('møtemappe', 'referanse_neste_moete__id', 'møtemappe', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS moetemappe__external_id_idx ON møtemappe(_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetemappe_system_id_idx ON møtemappe(system_id);
CREATE INDEX IF NOT EXISTS moetemappe_system_id_nonunique_idx ON møtemappe(system_id);
CREATE INDEX IF NOT EXISTS moetemappe_created_idx ON møtemappe(_created);
CREATE INDEX IF NOT EXISTS moetemappe_updated_idx ON møtemappe(_updated);
CREATE INDEX IF NOT EXISTS moetemappe_journalenhet__id ON møtemappe(journalenhet__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_moetemappe()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.møtemappe_iri IS NOT NULL AND NEW.møtemappe_iri != NEW._id THEN
    NEW._external_id := NEW.møtemappe_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_moetemappe_trigger ON møtemappe;
CREATE TRIGGER enrich_legacy_moetemappe_trigger BEFORE INSERT OR UPDATE ON møtemappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetemappe();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_moetemappe_journalenhet_trigger ON møtemappe;
CREATE TRIGGER enrich_legacy_moetemappe_journalenhet_trigger BEFORE INSERT OR UPDATE ON møtemappe
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


/* Behandlingsprotokoll */
CREATE TABLE IF NOT EXISTS behandlingsprotokoll(
  _id TEXT DEFAULT einnsyn_id('bp')
);
ALTER TABLE IF EXISTS behandlingsprotokoll
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('bp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS tekst_innhold TEXT,
  ADD COLUMN IF NOT EXISTS tekst_format TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('behandlingsprotokoll', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_id_idx ON behandlingsprotokoll(_id);
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_external_id_idx ON behandlingsprotokoll(_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_system_id_idx ON behandlingsprotokoll(system_id);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_created_idx ON behandlingsprotokoll(_created);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_updated_idx ON behandlingsprotokoll(_updated);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_journalenhet__id ON behandlingsprotokoll(journalenhet__id);


/* Vedtak */
CREATE TABLE IF NOT EXISTS vedtak(
  _id TEXT DEFAULT einnsyn_id('ved')
);
ALTER TABLE IF EXISTS vedtak
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ved'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtakstekst__id TEXT,
  ADD COLUMN IF NOT EXISTS behandlingsprotokoll__id TEXT,
  ADD COLUMN IF NOT EXISTS dato DATE,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('vedtak', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('vedtak', 'vedtakstekst__id', 'moetesaksbeskrivelse', '_id');
SELECT add_foreign_key_if_not_exists('vedtak', 'behandlingsprotokoll__id', 'behandlingsprotokoll', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_id_idx ON vedtak(_id);
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_external_id_idx ON vedtak(_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_system_id_idx ON vedtak(system_id);
CREATE INDEX IF NOT EXISTS vedtak_created_idx ON vedtak(_created);
CREATE INDEX IF NOT EXISTS vedtak_updated_idx ON vedtak(_updated);
CREATE INDEX IF NOT EXISTS vedtak_journalenhet__id ON vedtak(journalenhet__id);
CREATE INDEX IF NOT EXISTS vedtak_vedtakstekst__id ON vedtak(vedtakstekst__id);
CREATE INDEX IF NOT EXISTS vedtak_behandlingsprotokoll__id ON vedtak(behandlingsprotokoll__id);

CREATE TABLE IF NOT EXISTS vedtak_vedtaksdokument(
  vedtak__id TEXT,
  vedtaksdokument__id TEXT,
  CONSTRAINT vedtak_vedtaksdokument_pkey PRIMARY KEY (vedtak__id, vedtaksdokument__id),
  CONSTRAINT vedtak_vedtaksdokument_vedtak__id_fkey FOREIGN KEY (vedtak__id) REFERENCES vedtak(_id),
  CONSTRAINT vedtak_vedtaksdokument_vedtaksdokument__id_fkey FOREIGN KEY (vedtaksdokument__id) REFERENCES dokumentbeskrivelse(_id)
);


/* Utredning */
CREATE TABLE IF NOT EXISTS utredning(
  _id TEXT DEFAULT einnsyn_id('utr')
);
ALTER TABLE IF EXISTS utredning
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('utr'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS saksbeskrivelse__id TEXT,
  ADD COLUMN IF NOT EXISTS innstilling__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('utredning', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('utredning', 'saksbeskrivelse__id', 'moetesaksbeskrivelse', '_id');
SELECT add_foreign_key_if_not_exists('utredning', 'innstilling__id', 'moetesaksbeskrivelse', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS utredning_id_idx ON utredning (_id);
CREATE UNIQUE INDEX IF NOT EXISTS utredning_external_id_idx ON utredning (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS utredning_system_id_idx ON utredning (system_id);
CREATE INDEX IF NOT EXISTS utredning_created_idx ON utredning (_created);
CREATE INDEX IF NOT EXISTS utredning_updated_idx ON utredning (_updated);
CREATE INDEX IF NOT EXISTS utredning_journalenhet__id ON utredning(journalenhet__id);
CREATE INDEX IF NOT EXISTS utredning_saksbeskrivelse__id ON utredning(saksbeskrivelse__id);
CREATE INDEX IF NOT EXISTS utredning_innstilling__id ON utredning(innstilling__id);


/* Moetesaksregistrering */
CREATE TABLE IF NOT EXISTS møtesaksregistrering(
  _id TEXT DEFAULT einnsyn_id('ms')
);
ALTER TABLE IF EXISTS møtesaksregistrering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ms'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS utredning__id TEXT,
  ADD COLUMN IF NOT EXISTS innstilling__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtak__id TEXT,
  ADD COLUMN IF NOT EXISTS journalpost__id TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ALTER COLUMN møtemappe_id DROP NOT NULL, /* Eases insertion from parent møtemappe */
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'administrativ_enhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'utredning__id', 'utredning', '_id');
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'innstilling__id', 'moetesaksbeskrivelse', '_id');
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'vedtak__id', 'vedtak', '_id');
SELECT add_foreign_key_if_not_exists('møtesaksregistrering', 'journalpost__id', 'journalpost', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering_id_idx ON møtesaksregistrering (_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering__external_id_idx ON møtesaksregistrering (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering_system_id_idx ON møtesaksregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_system_id_nonunique_idx ON møtesaksregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_created_idx ON møtesaksregistrering (_created);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_updated_idx ON møtesaksregistrering (_updated);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_journalenhet__id ON møtesaksregistrering(journalenhet__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_administrativ_enhet__id ON møtesaksregistrering(administrativ_enhet__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_utredning__id ON møtesaksregistrering(utredning__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_innstilling__id ON møtesaksregistrering(innstilling__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_vedtak__id ON møtesaksregistrering(vedtak__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_journalpost__id ON møtesaksregistrering(journalpost__id);
-- TODO: This trigger should be removed when the legacy import is killed
CREATE OR REPLACE FUNCTION enrich_legacy_moetesaksregistrering()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL AND NEW.møtesaksregistrering_iri IS NOT NULL AND NEW.møtesaksregistrering_iri != NEW._id THEN
    NEW._external_id := NEW.møtesaksregistrering_iri;
  END IF;
  -- Look up adminiistrativ_enhet__id for old import
  IF TG_OP = 'UPDATE' AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper THEN
    SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksregistrering_trigger ON møtesaksregistrering;
CREATE TRIGGER enrich_legacy_moetesaksregistrering_trigger BEFORE INSERT OR UPDATE ON møtesaksregistrering
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetesaksregistrering();
-- look up journalenhet__id
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksregistrering_journalenhet_trigger ON møtesaksregistrering;
CREATE TRIGGER enrich_legacy_moetesaksregistrering_journalenhet_trigger BEFORE INSERT OR UPDATE ON møtesaksregistrering
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalenhet();


CREATE TABLE IF NOT EXISTS utredning_utredningsdokument(
  utredning__id TEXT,
  utredningsdokument__id TEXT,
  CONSTRAINT utredning_utredningsdokument_pkey PRIMARY KEY (utredning__id, utredningsdokument__id),
  CONSTRAINT utredning_utredningsdokument_utredning__id_fkey FOREIGN KEY (utredning__id) REFERENCES utredning(_id),
  CONSTRAINT utredning_utredningsdokument_utredningsdokument__id_fkey FOREIGN KEY (utredningsdokument__id) REFERENCES dokumentbeskrivelse(_id)
);


/* Identifikator */
CREATE TABLE IF NOT EXISTS identifikator(
  _id TEXT DEFAULT einnsyn_id('ide')
);
ALTER TABLE IF EXISTS identifikator
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ide'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS navn TEXT,
  ADD COLUMN IF NOT EXISTS identifikator TEXT,
  ADD COLUMN IF NOT EXISTS initialer TEXT,
  ADD COLUMN IF NOT EXISTS epostadresse TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('identifikator', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_id_idx ON identifikator (_id);
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_external_id_idx ON identifikator (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_system_id_idx ON identifikator (system_id);
CREATE INDEX IF NOT EXISTS identifikator_created_idx ON identifikator (_created);
CREATE INDEX IF NOT EXISTS identifikator_updated_idx ON identifikator (_updated);
CREATE INDEX IF NOT EXISTS identifikator_journalenhet__id ON identifikator(journalenhet__id);


/* Moetedeltaker */
CREATE TABLE IF NOT EXISTS moetedeltaker(
  _id TEXT DEFAULT einnsyn_id('md')
);
ALTER TABLE IF EXISTS moetedeltaker
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('md'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker_navn TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker_funksjon TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('moetedeltaker', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_id_idx ON moetedeltaker (_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_external_id_idx ON moetedeltaker (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_system_id_idx ON moetedeltaker (system_id);
CREATE INDEX IF NOT EXISTS moetedeltaker_created_idx ON moetedeltaker (_created);
CREATE INDEX IF NOT EXISTS moetedeltaker_updated_idx ON moetedeltaker (_updated);
CREATE INDEX IF NOT EXISTS moetedeltaker_journalenhet__id ON moetedeltaker(journalenhet__id);


/* Votering */
CREATE TABLE IF NOT EXISTS votering(
  _id TEXT DEFAULT einnsyn_id('vot')
);
ALTER TABLE IF EXISTS votering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('vot'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker__id TEXT,
  ADD COLUMN IF NOT EXISTS representerer__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtak__id TEXT,
  ADD COLUMN IF NOT EXISTS stemme TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
SELECT add_foreign_key_if_not_exists('votering', 'journalenhet__id', 'enhet', '_id');
SELECT add_foreign_key_if_not_exists('votering', 'moetedeltaker__id', 'moetedeltaker', '_id');
SELECT add_foreign_key_if_not_exists('votering', 'representerer__id', 'identifikator', '_id');
SELECT add_foreign_key_if_not_exists('votering', 'vedtak__id', 'vedtak', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS votering_id_idx ON votering (_id);
CREATE UNIQUE INDEX IF NOT EXISTS votering_external_id_idx ON votering (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS votering_system_id_idx ON votering (system_id);
CREATE INDEX IF NOT EXISTS votering_created_idx ON votering (_created);
CREATE INDEX IF NOT EXISTS votering_updated_idx ON votering (_updated);
CREATE INDEX IF NOT EXISTS votering_journalenhet__id ON votering(journalenhet__id);
CREATE INDEX IF NOT EXISTS votering_moetedeltaker__id ON votering(moetedeltaker__id);
CREATE INDEX IF NOT EXISTS votering_representerer__id ON votering(representerer__id);
CREATE INDEX IF NOT EXISTS votering_vedtak__id ON votering(vedtak__id);


/* Bruker */
ALTER TABLE IF EXISTS bruker
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('bru'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS language TEXT DEFAULT 'nb';
SELECT add_foreign_key_if_not_exists('bruker', 'journalenhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS bruker__id_idx ON bruker (_id);
CREATE UNIQUE INDEX IF NOT EXISTS bruker__external_id_idx ON bruker (_external_id);
CREATE INDEX IF NOT EXISTS bruker__created_idx ON bruker (_created);
CREATE INDEX IF NOT EXISTS bruker__updated_idx ON bruker (_updated);


/* Innsynskrav */
ALTER TABLE IF EXISTS innsynskrav
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ik'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS language TEXT,
  ADD COLUMN IF NOT EXISTS locked BOOLEAN DEFAULT false,
  ADD COLUMN IF NOT EXISTS bruker__id TEXT,
  ADD COLUMN IF NOT EXISTS innsynskrav_version INT DEFAULT 0; -- Used to differentiate between innsynskrav from the old/new api
SELECT add_foreign_key_if_not_exists('innsynskrav', 'bruker__id', 'bruker', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav__id_idx ON innsynskrav (_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav__external_id_idx ON innsynskrav (_external_id);
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
  ADD COLUMN IF NOT EXISTS journalpost__id TEXT,
  ADD COLUMN IF NOT EXISTS enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS sent TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS retry_timestamp TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1;
SELECT add_foreign_key_if_not_exists('innsynskrav_del', 'journalpost__id', 'journalpost', '_id');
SELECT add_foreign_key_if_not_exists('innsynskrav_del', 'enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del__id_idx ON innsynskrav_del (_id);
CREATE UNIQUE INDEX IF NOT EXISTS innsynskrav_del__external_id_idx ON innsynskrav_del (_external_id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_journalpost_idx ON innsynskrav_del (journalpost__id);
CREATE INDEX IF NOT EXISTS innsynskrav_del_enhet_idx ON innsynskrav_del (enhet__id);
CREATE INDEX IF NOT EXISTS innsynskrav_del__created_idx ON innsynskrav_del (_created);
CREATE INDEX IF NOT EXISTS innsynskrav_del__updated_idx ON innsynskrav_del (_updated);
CREATE INDEX IF NOT EXISTS innsynskrav_del_retries ON innsynskrav_del(sent, innsynskrav_id, retry_timestamp, retry_count);
/* TODO: Lookup journalpost__id, enhet__id from old entries, add not null after */


/* Tilbakemelding */
CREATE TABLE IF NOT EXISTS tilbakemelding(
    _id TEXT DEFAULT einnsyn_id('tbm'),
    _external_id TEXT,
    _created TIMESTAMP DEFAULT now(),
    _updated TIMESTAMP DEFAULT now(),
    lock_version BIGINT NOT NULL DEFAULT 1,
    message_from_user TEXT,
    path TEXT,
    referer TEXT,
    user_agent TEXT,
    screen_height INT,
    screen_width INT,
    doc_height INT,
    doc_width INT,
    win_height INT,
    win_width INT,
    scroll_x INT,
    scroll_y INT,
    user_satisfied BOOLEAN,
    handled_by_admin BOOLEAN,
    admin_comment TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS tilbakemelding__id_idx ON tilbakemelding (_id);
CREATE INDEX IF NOT EXISTS tilbakemelding__created_idx ON tilbakemelding (_created);
CREATE INDEX IF NOT EXISTS tilbakemelding__updated_idx ON tilbakemelding (_updated);


/* ApiKey */
CREATE TABLE IF NOT EXISTS api_key(
  _id TEXT DEFAULT einnsyn_id('key')
);
ALTER TABLE IF EXISTS api_key
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('key'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS name TEXT,
  ADD COLUMN IF NOT EXISTS secret TEXT,
  ADD COLUMN IF NOT EXISTS enhet__id TEXT;
SELECT add_foreign_key_if_not_exists('api_key', 'enhet__id', 'enhet', '_id');
CREATE UNIQUE INDEX IF NOT EXISTS api_key_id_idx ON api_key (_id);
CREATE UNIQUE INDEX IF NOT EXISTS api_key_external_id_idx ON api_key (_external_id);
CREATE INDEX IF NOT EXISTS api_key_created_idx ON api_key (_created);
CREATE INDEX IF NOT EXISTS api_key_updated_idx ON api_key (_updated);
CREATE INDEX IF NOT EXISTS api_key_enhet_id_idx ON api_key (enhet__id);

/* Insert root enhet with an API key if it doesn't exist */
DROP EXTENSION IF EXISTS "pgcrypto";
CREATE EXTENSION "pgcrypto";

DROP EXTENSION IF EXISTS "uuid-ossp";
CREATE EXTENSION "uuid-ossp";

DO $$
DECLARE
  rootEnhetId UUID;
  rootEnhet_Id VARCHAR;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM enhet WHERE _external_id = 'root') THEN
    /* Insert root enhet */
    INSERT INTO enhet (
      id,
      navn,
      _external_id,
      iri,
      type
    )
    VALUES (
      uuid_generate_v4(),
      'Root enhet',
      'root',
      'http://data.einnsyn.no/virksomhet/root',
      'DUMMYENHET'
    );
    SELECT _id, id INTO rootEnhet_Id, rootEnhetId FROM enhet WHERE _external_id = 'root';

    /* Set root enhet as parent of previous parents */
    UPDATE enhet SET parent_id = rootEnhetId WHERE parent_id IS NULL AND _id != rootEnhet_Id;

    /* Insert API key for root enhet */
    INSERT INTO api_key (name, secret, enhet__id)
    VALUES (
      'Root API key',
      encode(digest('${apikey-root-key}', 'sha256'), 'hex'),
      rootEnhet_Id
    );
  END IF;
END
$$
