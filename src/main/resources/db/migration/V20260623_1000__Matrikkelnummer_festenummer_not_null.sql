UPDATE matrikkelnummer SET festenummer = 0 WHERE festenummer IS NULL;
UPDATE matrikkelnummer SET seksjonsnummer = 0 WHERE seksjonsnummer IS NULL;

ALTER TABLE matrikkelnummer
  ALTER COLUMN festenummer SET NOT NULL,
  ALTER COLUMN festenummer SET DEFAULT 0,
  ALTER COLUMN seksjonsnummer SET NOT NULL,
  ALTER COLUMN seksjonsnummer SET DEFAULT 0;
