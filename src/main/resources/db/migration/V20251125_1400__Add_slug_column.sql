-- Add slug column to tables implementing HasSlug interface

ALTER TABLE enhet
    ADD COLUMN IF NOT EXISTS slug TEXT;

ALTER TABLE saksmappe
    ADD COLUMN IF NOT EXISTS slug TEXT;

ALTER TABLE møtemappe
    ADD COLUMN IF NOT EXISTS slug TEXT;

ALTER TABLE journalpost
    ADD COLUMN IF NOT EXISTS slug TEXT;

ALTER TABLE møtesaksregistrering
    ADD COLUMN IF NOT EXISTS slug TEXT;

ALTER TABLE møtedokumentregistrering
    ADD COLUMN IF NOT EXISTS slug TEXT;

-- Add unique indices for slug columns

CREATE UNIQUE INDEX IF NOT EXISTS enhet_slug_idx ON enhet (slug);

CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_slug_idx ON saksmappe (slug);

CREATE UNIQUE INDEX IF NOT EXISTS møtemappe_slug_idx ON møtemappe (slug);

CREATE UNIQUE INDEX IF NOT EXISTS journalpost_slug_idx ON journalpost (slug);

CREATE UNIQUE INDEX IF NOT EXISTS møtesaksregistrering_slug_idx ON møtesaksregistrering (slug);

CREATE UNIQUE INDEX IF NOT EXISTS møtedokumentregistrering_slug_idx ON møtedokumentregistrering (slug);
