CREATE SEQUENCE IF NOT EXISTS matrikkelnummer_seq INCREMENT 1 START 1;

CREATE TABLE IF NOT EXISTS matrikkelnummer(
  matrikkelnummer_id int PRIMARY KEY DEFAULT nextval('matrikkelnummer_seq'),
  kommunenummer text NOT NULL,
  gaardsnummer int NOT NULL,
  bruksnummer int NOT NULL,
  festenummer int,
  seksjonsnummer int,
  _id text COLLATE "C" DEFAULT einnsyn_id('mat'),
  _external_id text,
  _created timestamptz DEFAULT now(),
  _updated timestamptz DEFAULT now(),
  _accessible_after timestamptz DEFAULT now(),
  lock_version bigint,
  mappe__id text COLLATE "C",
  registrering__id text COLLATE "C",
  CONSTRAINT matrikkelnummer_parent_check CHECK (
    (mappe__id IS NOT NULL AND registrering__id IS NULL)
    OR (mappe__id IS NULL AND registrering__id IS NOT NULL)
  )
);

CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__id_idx ON matrikkelnummer (_id);
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__external_id_idx
  ON matrikkelnummer (_external_id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_mappe__id_idx ON matrikkelnummer (mappe__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_registrering__id_idx
  ON matrikkelnummer (registrering__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer__created_idx ON matrikkelnummer (_created);
CREATE INDEX IF NOT EXISTS matrikkelnummer__updated_idx ON matrikkelnummer (_updated);

CREATE OR REPLACE FUNCTION validate_matrikkelnummer_parent()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.mappe__id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM saksmappe WHERE _id = NEW.mappe__id)
      AND NOT EXISTS (SELECT 1 FROM møtemappe WHERE _id = NEW.mappe__id) THEN
    RAISE foreign_key_violation
      USING MESSAGE = 'matrikkelnummer.mappe__id does not reference a mappe';
  END IF;

  IF NEW.registrering__id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM journalpost WHERE _id = NEW.registrering__id)
      AND NOT EXISTS (
        SELECT 1 FROM møtesaksregistrering WHERE _id = NEW.registrering__id
      )
      AND NOT EXISTS (
        SELECT 1 FROM møtedokumentregistrering WHERE _id = NEW.registrering__id
      ) THEN
    RAISE foreign_key_violation
      USING MESSAGE = 'matrikkelnummer.registrering__id does not reference a registrering';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS validate_matrikkelnummer_parent_trigger ON matrikkelnummer;
CREATE CONSTRAINT TRIGGER validate_matrikkelnummer_parent_trigger
AFTER INSERT OR UPDATE OF mappe__id, registrering__id ON matrikkelnummer
DEFERRABLE INITIALLY IMMEDIATE
FOR EACH ROW EXECUTE FUNCTION validate_matrikkelnummer_parent();

CREATE OR REPLACE FUNCTION delete_matrikkelnummer_for_mappe()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM matrikkelnummer WHERE mappe__id = OLD._id;
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_matrikkelnummer_for_registrering()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM matrikkelnummer WHERE registrering__id = OLD._id;
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS delete_matrikkelnummer_for_saksmappe_trigger ON saksmappe;
CREATE TRIGGER delete_matrikkelnummer_for_saksmappe_trigger
AFTER DELETE ON saksmappe
FOR EACH ROW EXECUTE FUNCTION delete_matrikkelnummer_for_mappe();

DROP TRIGGER IF EXISTS delete_matrikkelnummer_for_moetemappe_trigger ON møtemappe;
CREATE TRIGGER delete_matrikkelnummer_for_moetemappe_trigger
AFTER DELETE ON møtemappe
FOR EACH ROW EXECUTE FUNCTION delete_matrikkelnummer_for_mappe();

DROP TRIGGER IF EXISTS delete_matrikkelnummer_for_journalpost_trigger ON journalpost;
CREATE TRIGGER delete_matrikkelnummer_for_journalpost_trigger
AFTER DELETE ON journalpost
FOR EACH ROW EXECUTE FUNCTION delete_matrikkelnummer_for_registrering();

DROP TRIGGER IF EXISTS delete_matrikkelnummer_for_moetesak_trigger ON møtesaksregistrering;
CREATE TRIGGER delete_matrikkelnummer_for_moetesak_trigger
AFTER DELETE ON møtesaksregistrering
FOR EACH ROW EXECUTE FUNCTION delete_matrikkelnummer_for_registrering();

DROP TRIGGER IF EXISTS delete_matrikkelnummer_for_moetedokument_trigger
  ON møtedokumentregistrering;
CREATE TRIGGER delete_matrikkelnummer_for_moetedokument_trigger
AFTER DELETE ON møtedokumentregistrering
FOR EACH ROW EXECUTE FUNCTION delete_matrikkelnummer_for_registrering();
