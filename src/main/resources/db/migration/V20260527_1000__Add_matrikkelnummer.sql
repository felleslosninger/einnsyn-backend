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
  journalenhet__id text COLLATE "C" REFERENCES enhet (_id),
  system_id text,
  virksomhet_iri text,
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
CREATE INDEX IF NOT EXISTS matrikkelnummer_journalenhet__id_idx ON matrikkelnummer (journalenhet__id);
CREATE INDEX IF NOT EXISTS matrikkelnummer_kommunenummer_idx ON matrikkelnummer (kommunenummer);
CREATE INDEX IF NOT EXISTS matrikkelnummer_gaardsnummer_idx ON matrikkelnummer (gaardsnummer);
CREATE INDEX IF NOT EXISTS matrikkelnummer_bruksnummer_idx ON matrikkelnummer (bruksnummer);

CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_saksmappe_unique_idx
  ON matrikkelnummer (saksmappe__id, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer)
  WHERE saksmappe__id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_moetemappe_unique_idx
  ON matrikkelnummer (moetemappe__id, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer)
  WHERE moetemappe__id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_journalpost_unique_idx
  ON matrikkelnummer (journalpost__id, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer)
  WHERE journalpost__id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_moetesak_unique_idx
  ON matrikkelnummer (moetesak__id, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer)
  WHERE moetesak__id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_moetedokument_unique_idx
  ON matrikkelnummer (moetedokument__id, kommunenummer, gaardsnummer, bruksnummer, festenummer, seksjonsnummer)
  WHERE moetedokument__id IS NOT NULL;

ALTER TABLE matrikkelnummer ADD CONSTRAINT matrikkelnummer_exactly_one_parent CHECK (
  (CASE WHEN saksmappe__id IS NOT NULL THEN 1 ELSE 0 END +
   CASE WHEN moetemappe__id IS NOT NULL THEN 1 ELSE 0 END +
   CASE WHEN journalpost__id IS NOT NULL THEN 1 ELSE 0 END +
   CASE WHEN moetesak__id IS NOT NULL THEN 1 ELSE 0 END +
   CASE WHEN moetedokument__id IS NOT NULL THEN 1 ELSE 0 END) = 1
);
