DROP INDEX IF EXISTS matrikkelnummer_unique_fields_idx;

CREATE UNIQUE INDEX IF NOT EXISTS matrikkelnummer_unique_fields_idx
  ON matrikkelnummer (
    journalenhet__id,
    kommunenummer,
    gaardsnummer,
    bruksnummer,
    COALESCE(festenummer, -1),
    COALESCE(seksjonsnummer, -1)
  );
