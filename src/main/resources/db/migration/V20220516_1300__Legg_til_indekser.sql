CREATE INDEX IF NOT EXISTS journalpost_publisert_dato_idx
  ON journalpost (publisert_dato);
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_virksomhet_iri_journalpost_iri_idx
  ON journalpost (virksomhet_iri, journalpost_iri);
CREATE INDEX IF NOT EXISTS journalpost_journaldato_idx
  ON journalpost (journaldato);

CREATE INDEX IF NOT EXISTS møtesaksregistrering_publisert_dato_idx
  ON møtesaksregistrering (publisert_dato);
CREATE UNIQUE INDEX IF NOT EXISTS møtesaksregistrering_virksomhet_iri_møtesaksregistrering_iri_idx
  ON møtesaksregistrering (virksomhet_iri, møtesaksregistrering_iri);

CREATE INDEX IF NOT EXISTS møtedokumentregistrering_publisert_dato_idx
  ON møtedokumentregistrering (publisert_dato);
CREATE UNIQUE INDEX IF NOT EXISTS møtedokumentregistrering_virksomhet_iri_møtedokumentregistrering_iri_idx
  ON møtedokumentregistrering (virksomhet_iri, møtedokumentregistrering_iri);

CREATE INDEX IF NOT EXISTS saksmappe_publisert_dato_idx
  ON saksmappe (publisert_dato);
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_virksomhet_iri_saksmappe_iri_idx
  ON saksmappe (virksomhet_iri, saksmappe_iri);

CREATE INDEX IF NOT EXISTS møtemappe_publisert_dato_idx
  ON møtemappe (publisert_dato);
CREATE UNIQUE INDEX IF NOT EXISTS møtemappe_virksomhet_iri_møtemappe_iri_idx
  ON møtemappe (virksomhet_iri, møtemappe_iri);
