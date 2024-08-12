-- Add dummy _id columns for old API. In new environemnts, these columns are added by the API.
ALTER TABLE
  saksmappe
ADD
  COLUMN IF NOT EXISTS _id TEXT;

ALTER TABLE
  journalpost
ADD
  COLUMN IF NOT EXISTS _id TEXT;

ALTER TABLE
  møtemappe
ADD
  COLUMN IF NOT EXISTS _id TEXT;

ALTER TABLE
  møtesaksregistrering
ADD
  COLUMN IF NOT EXISTS _id TEXT;

ALTER TABLE
  møtedokumentregistrering
ADD
  COLUMN IF NOT EXISTS _id TEXT;

ALTER TABLE
  dokumentobjekt
ADD
  COLUMN IF NOT EXISTS _id TEXT;
