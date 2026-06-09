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
  saksmappe__id text COLLATE "C" REFERENCES saksmappe (_id),
  moetemappe__id text COLLATE "C" REFERENCES møtemappe (_id),
  journalpost__id text COLLATE "C" REFERENCES journalpost (_id),
  moetesak__id text COLLATE "C" REFERENCES møtesaksregistrering (_id),
  moetedokument__id text COLLATE "C" REFERENCES møtedokumentregistrering (_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__id_idx ON matrikkelnummer (_id);
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer__external_id_idx
  ON matrikkelnummer (_external_id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_saksmappe__id_idx ON matrikkelnummer (saksmappe__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_moetemappe__id_idx ON matrikkelnummer (moetemappe__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_journalpost__id_idx ON matrikkelnummer (journalpost__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_moetesak__id_idx ON matrikkelnummer (moetesak__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_moetedokument__id_idx ON matrikkelnummer (moetedokument__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer__created_idx ON matrikkelnummer (_created);
CREATE INDEX IF NOT EXISTS matrikkelnummer__updated_idx ON matrikkelnummer (_updated);
