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
  system_id text,
  virksomhet_iri text,
  journalenhet__id text NOT NULL
);

SELECT add_foreign_key_if_not_exists('matrikkelnummer', 'journalenhet__id', 'enhet', '_id');

CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__id_idx ON matrikkelnummer (_id);
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__external_id_idx
  ON matrikkelnummer (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_system_id_idx ON matrikkelnummer (system_id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_journalenhet_idx ON matrikkelnummer (journalenhet__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer__created_idx ON matrikkelnummer (_created);
CREATE INDEX IF NOT EXISTS matrikkelnummer__updated_idx ON matrikkelnummer (_updated);
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_unique_fields_idx
  ON matrikkelnummer (
    journalenhet__id,
    kommunenummer,
    gaardsnummer,
    bruksnummer,
    COALESCE(festenummer, -1),
    COALESCE(seksjonsnummer, -1)
  );

CREATE TABLE IF NOT EXISTS journalpost_matrikkelnummer(
  journalpost_id int NOT NULL,
  matrikkelnummer_id int NOT NULL,
  PRIMARY KEY (journalpost_id, matrikkelnummer_id),
  CONSTRAINT journalpost_matrikkelnummer_journalpost_id_fkey
    FOREIGN KEY (journalpost_id) REFERENCES journalpost(journalpost_id) ON DELETE CASCADE,
  CONSTRAINT journalpost_matrikkelnummer_matrikkelnummer_id_fkey
    FOREIGN KEY (matrikkelnummer_id) REFERENCES matrikkelnummer(matrikkelnummer_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS journalpost_matrikkelnummer_matrikkelnummer_id_idx
  ON journalpost_matrikkelnummer (matrikkelnummer_id);

CREATE TABLE IF NOT EXISTS saksmappe_matrikkelnummer(
  saksmappe_id int NOT NULL,
  matrikkelnummer_id int NOT NULL,
  PRIMARY KEY (saksmappe_id, matrikkelnummer_id),
  CONSTRAINT saksmappe_matrikkelnummer_saksmappe_id_fkey
    FOREIGN KEY (saksmappe_id) REFERENCES saksmappe(saksmappe_id) ON DELETE CASCADE,
  CONSTRAINT saksmappe_matrikkelnummer_matrikkelnummer_id_fkey
    FOREIGN KEY (matrikkelnummer_id) REFERENCES matrikkelnummer(matrikkelnummer_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS saksmappe_matrikkelnummer_matrikkelnummer_id_idx
  ON saksmappe_matrikkelnummer (matrikkelnummer_id);
