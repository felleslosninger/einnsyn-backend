CREATE SEQUENCE IF NOT EXISTS einnsyn_object_seq;

/* If we use InheritanceType.JOINED: */
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

/* If we use MappedSuperclass: */
/*
ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS internal_id BIGINT PRIMARY KEY DEFAULT nextval('einnsyn_object'),
  ADD COLUMN IF NOT EXISTS entity TEXT NOT NULL,
  ADD COLUMN IF NOT EXISTS external_id TEXT,
  ADD COLUMN IF NOT EXISTS created TIMESTAMP NOT NULL,
  ADD COLUMN IF NOT EXISTS updated TIMESTAMP NOT NULL,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL;
*/