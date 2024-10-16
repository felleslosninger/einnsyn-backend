-- Postgres-spesifikk migrering, for korrekt sortering av IRI.
-- H2 støtter ikke collate per kolonne.

ALTER TABLE IF EXISTS enhet
ALTER COLUMN iri SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";

ALTER TABLE IF EXISTS innsynskrav
    ALTER COLUMN bruker_iri SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";

ALTER TABLE IF EXISTS innsynskrav_del
    ALTER COLUMN rettet_mot SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";
ALTER TABLE IF EXISTS innsynskrav_del
    ALTER COLUMN virksomhet SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";

ALTER TABLE IF EXISTS lagret_sak
    ALTER COLUMN sak_id SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";

ALTER TABLE IF EXISTS lagret_sok_treff
    ALTER COLUMN rettet_mot SET DATA TYPE varchar(255) COLLATE "nb-NO-x-icu";
