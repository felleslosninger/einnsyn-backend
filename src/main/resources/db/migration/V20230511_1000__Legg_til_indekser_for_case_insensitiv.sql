CREATE UNIQUE INDEX IF NOT EXISTS jp_virksomhet_iri_upper_jp_iri_idx
  ON journalpost (virksomhet_iri, UPPER(journalpost_iri));

CREATE UNIQUE INDEX IF NOT EXISTS msr_virksomhet_iri_upper_msr_iri_idx
  ON møtesaksregistrering (virksomhet_iri, UPPER(møtesaksregistrering_iri));

CREATE UNIQUE INDEX IF NOT EXISTS mdr_virksomhet_iri_upper_mdr_iri_idx
  ON møtedokumentregistrering (virksomhet_iri, UPPER(møtedokumentregistrering_iri));

CREATE UNIQUE INDEX IF NOT EXISTS sm_virksomhet_iri_upper_sm_iri_idx
  ON saksmappe (virksomhet_iri, UPPER(saksmappe_iri));

CREATE UNIQUE INDEX IF NOT EXISTS mm_virksomhet_iri_upper_mm_iri_idx
  ON møtemappe (virksomhet_iri, UPPER(møtemappe_iri));
