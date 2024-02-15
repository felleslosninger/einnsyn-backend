CREATE INDEX IF NOT EXISTS skjerming_skjerminghjemmel_tilgangsrestr_idx
  ON skjerming (skjerming_iri, skjermingshjemmel, tilgangsrestriksjon);

CREATE INDEX IF NOT EXISTS dokumentobjekt_dokumentobj_iri_dokbeskr_iri_idx
  ON dokumentobjekt (dokumentobjekt_iri, dokumentbeskrivelse_iri);
