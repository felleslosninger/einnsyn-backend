ALTER TABLE dokumentbeskrivelse
  ADD COLUMN IF NOT EXISTS virksomhet_iri text;

CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse_virksomhet_iri_dokumentbeskrivelse_iri_idx
  ON dokumentbeskrivelse (virksomhet_iri, dokumentbeskrivelse_iri);
