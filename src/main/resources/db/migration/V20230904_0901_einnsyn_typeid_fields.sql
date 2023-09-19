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
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('sm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP;
UPDATE saksmappe SET _created = publisert_dato WHERE _created IS NULL;
UPDATE saksmappe SET _created = now() WHERE _created IS NULL;
UPDATE saksmappe SET _updated = publisert_dato WHERE _updated IS NULL;
UPDATE saksmappe SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS saksmappe
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS saksmappe_id_idx ON saksmappe (_id);

/* Journalpost */
ALTER TABLE IF EXISTS journalpost
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('jp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP;
UPDATE journalpost SET _created = publisert_dato WHERE _created IS NULL;
UPDATE journalpost SET _created = now() WHERE _created IS NULL;
UPDATE journalpost SET _updated = publisert_dato WHERE _updated IS NULL;
UPDATE journalpost SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS journalpost
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS journalpost_id_idx ON journalpost (_id);

/* Enhet */
ALTER TABLE IF EXISTS enhet
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('enhet'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP,
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP;
UPDATE enhet SET _created = opprettet_dato WHERE _created IS NULL;
UPDATE enhet SET _created = now() WHERE _created IS NULL;
UPDATE enhet SET _updated = oppdatert_dato WHERE _updated IS NULL;
UPDATE enhet SET _updated = now() WHERE _updated IS NULL;
ALTER TABLE IF EXISTS enhet
  ALTER COLUMN _created SET DEFAULT now(),
  ALTER COLUMN _updated SET DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS enhet_id_idx ON enhet (_id);

/* Skjerming */
ALTER TABLE IF EXISTS skjerming
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('skj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS skjerming_id_idx ON skjerming (_id);

/* Korrespondansepart */
ALTER TABLE IF EXISTS korrespondansepart
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kpart'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS korrespondansepart_id_idx ON korrespondansepart (_id);

/* Dokumentbeskrivelse */
ALTER TABLE IF EXISTS dokumentbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokbesk'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS virksomhet_id UUID NOT NULL REFERENCES enhet(id) ON DELETE CASCADE;
CREATE UNIQUE INDEX IF NOT EXISTS dokumentbeskrivelse_id_idx ON dokumentbeskrivelse (_id);

/* Dokumentobjekt */
ALTER TABLE IF EXISTS dokumentobjekt
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('dokobj'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMP DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMP DEFAULT now();
CREATE UNIQUE INDEX IF NOT EXISTS dokumentobjekt_id_idx ON dokumentobjekt (_id);