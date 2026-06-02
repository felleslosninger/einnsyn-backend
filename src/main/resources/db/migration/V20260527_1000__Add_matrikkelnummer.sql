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
  lock_version bigint NOT NULL DEFAULT 1,
  mappe__id text COLLATE "C",
  registrering__id text COLLATE "C"
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
DECLARE
  current_mappe__id text;
  current_registrering__id text;
BEGIN
  SELECT mappe__id, registrering__id
  INTO current_mappe__id, current_registrering__id
  FROM matrikkelnummer
  WHERE matrikkelnummer_id = NEW.matrikkelnummer_id;

  IF NOT FOUND THEN
    RETURN NEW;
  END IF;

  IF current_mappe__id IS NULL AND current_registrering__id IS NULL THEN
    RAISE foreign_key_violation
      USING MESSAGE = 'matrikkelnummer must reference either a mappe or a registrering';
  END IF;

  IF current_mappe__id IS NOT NULL AND current_registrering__id IS NOT NULL THEN
    RAISE foreign_key_violation
      USING MESSAGE = 'matrikkelnummer cannot reference both mappe and registrering';
  END IF;

  IF current_mappe__id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM saksmappe WHERE _id = current_mappe__id)
      AND NOT EXISTS (SELECT 1 FROM møtemappe WHERE _id = current_mappe__id) THEN
    RAISE foreign_key_violation
      USING MESSAGE = 'matrikkelnummer.mappe__id does not reference a mappe';
  END IF;

  IF current_registrering__id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM journalpost WHERE _id = current_registrering__id)
      AND NOT EXISTS (
        SELECT 1 FROM møtesaksregistrering WHERE _id = current_registrering__id
      )
      AND NOT EXISTS (
        SELECT 1 FROM møtedokumentregistrering WHERE _id = current_registrering__id
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
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_matrikkelnummer_parent();
