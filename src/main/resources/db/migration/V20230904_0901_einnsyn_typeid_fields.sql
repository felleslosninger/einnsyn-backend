CREATE SEQUENCE IF NOT EXISTS einnsyn_object_seq;

/* If we use InheritanceType.JOINED: */
/*
CREATE TABLE IF NOT EXISTS einnsyn_object (
    internal_id BIGINT PRIMARY KEY default nextval('einnsyn_object'),
    id TEXT NOT NULL,
    entity TEXT NOT NULL,
    external_id TEXT,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    lock_version BIGINT NOT NULL
);

ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS internal_id BIGINT;

ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS internal_id BIGINT;
*/

/* If we use MappedSuperclass: */
ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS id TEXT,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS external_id TEXT,
  ADD COLUMN IF NOT EXISTS created TIMESTAMP,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS updated TIMESTAMP,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL;
UPDATE saksmappe SET created = publisert_dato WHERE created IS NULL;
UPDATE saksmappe SET created = now() WHERE created IS NULL;
UPDATE saksmappe SET updated = publisert_dato WHERE updated IS NULL;
UPDATE saksmappe SET updated = now() WHERE updated IS NULL;
ALTER TABLE IF EXISTS saksmappe
  ALTER COLUMN created SET NOT NULL,
  ALTER COLUMN updated SET NOT NULL;
/* TODO: Generate typeIds for existing rows */
CREATE UNIQUE INDEX IF NOT EXISTS id_idx ON saksmappe (id);

/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS id TEXT,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS external_id TEXT,
  ADD COLUMN IF NOT EXISTS created TIMESTAMP,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS updated TIMESTAMP,/* NOT NULL, */
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL;
UPDATE journalpost SET created = publisert_dato WHERE created IS NULL;
UPDATE journalpost SET created = now() WHERE created IS NULL;
UPDATE journalpost SET updated = publisert_dato WHERE updated IS NULL;
UPDATE journalpost SET updated = now() WHERE updated IS NULL;
ALTER TABLE IF EXISTS journalpost
  ALTER COLUMN created SET NOT NULL,
  ALTER COLUMN updated SET NOT NULL;
/* TODO: Generate typeIds for existing rows */
CREATE UNIQUE INDEX IF NOT EXISTS id_idx ON journalpost (id);