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
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.arkiv_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_arkiv_trigger ON arkiv;
CREATE TRIGGER enrich_legacy_arkiv_trigger BEFORE INSERT OR UPDATE ON arkiv
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_arkiv();


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
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.arkivdel_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_arkivdel_trigger ON arkivdel;
CREATE TRIGGER enrich_legacy_arkivdel_trigger BEFORE INSERT OR UPDATE ON arkivdel
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_arkivdel();


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
  IF NEW._external_id IS NULL AND OLD._external_id IS NULL THEN
    NEW._external_id = OLD.klasse_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_klasse_trigger ON klasse;
CREATE TRIGGER enrich_legacy_klasse_trigger BEFORE INSERT OR UPDATE ON klasse
  FOR EACH ROW EXECUTE FUNCTION enrich_legacy_klasse();


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
CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_id_idx ON klassifikasjonssystem (_id);
CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_external_id_idx ON klassifikasjonssystem (_external_id);
/*CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_system_id_idx ON klassifikasjonssystem (system_id);*/
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_system_id_nonunique_idx ON klassifikasjonssystem (system_id);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_created_idx ON klassifikasjonssystem (_created);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_updated_idx ON klassifikasjonssystem (_updated);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_journalenhet__id ON klassifikasjonssystem(journalenhet__id);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_arkivdel ON klassifikasjonssystem(arkivdel__id);
