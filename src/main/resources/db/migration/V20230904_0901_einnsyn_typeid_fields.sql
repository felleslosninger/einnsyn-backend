-- ID generator
create or replace function einnsyn_id(prefix text)
returns text
as $$
declare
  uuid uuid;
  suffix text;
begin
  if (prefix is null) or not (prefix ~ '^[a-z]{0,63}$') then
    raise exception 'prefix must match the regular expression [a-z]{0,63}';
  end if;
  uuid = uuid_generate_v7();
  suffix = base32_encode(uuid);
  return (prefix || '_' || suffix);
end
$$
language plpgsql
volatile;



/* Saksmappe */
ALTER TABLE IF EXISTS saksmappe
  ADD COLUMN IF NOT EXISTS id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS external_id TEXT,
  ADD COLUMN IF NOT EXISTS created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated TIMESTAMP;
UPDATE saksmappe SET created = publisert_dato WHERE created IS NULL;
UPDATE saksmappe SET created = now() WHERE created IS NULL;
UPDATE saksmappe SET updated = publisert_dato WHERE updated IS NULL;
UPDATE saksmappe SET updated = now() WHERE updated IS NULL;
ALTER TABLE IF EXISTS saksmappe
  ALTER COLUMN created SET DEFAULT now(),
  ALTER COLUMN updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS id_idx ON saksmappe (id);

/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS external_id TEXT,
  ADD COLUMN IF NOT EXISTS created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated TIMESTAMP,
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL;
UPDATE journalpost SET created = publisert_dato WHERE created IS NULL;
UPDATE journalpost SET created = now() WHERE created IS NULL;
UPDATE journalpost SET updated = publisert_dato WHERE updated IS NULL;
UPDATE journalpost SET updated = now() WHERE updated IS NULL;
ALTER TABLE IF EXISTS journalpost
  ALTER COLUMN created SET DEFAULT now(),
  ALTER COLUMN updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS id_idx ON journalpost (id);