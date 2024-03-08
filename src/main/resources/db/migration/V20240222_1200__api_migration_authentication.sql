/* ApiKey */
CREATE TABLE IF NOT EXISTS api_key(
  _id TEXT DEFAULT einnsyn_id('key')
);
ALTER TABLE IF EXISTS api_key
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('key'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS name TEXT,
  ADD COLUMN IF NOT EXISTS secret TEXT,
  ADD COLUMN IF NOT EXISTS enhet__id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS api_key_id_idx ON api_key (_id);
CREATE UNIQUE INDEX IF NOT EXISTS api_key_external_id_idx ON api_key (_external_id);
CREATE INDEX IF NOT EXISTS api_key_enhet_id_idx ON api_key (enhet__id);

/* Insert root enhet with an API key if it doesn't exist */
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
DO $$
DECLARE
  rootEnhetId UUID;
  rootEnhet_Id VARCHAR;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM enhet WHERE _external_id = 'root') THEN
    /* Insert root enhet */
    INSERT INTO enhet (
      id,
      navn,
      _external_id,
      iri,
      type
    )
    VALUES (
      uuid_generate_v4(),
      'Root enhet',
      'root',
      'root',
      'DUMMYENHET'
    );
    SELECT _id, id INTO rootEnhet_Id, rootEnhetId FROM enhet WHERE _external_id = 'root';

    /* Set root enhet as parent of previous parents */
    UPDATE enhet SET parent_id = rootEnhetId WHERE parent_id IS NULL;

    /* Insert API key for root enhet */
    INSERT INTO api_key (name, _id, secret, enhet__id)
    VALUES (
      'Root API key',
      '${apikey-root-key}',
      crypt('${apikey-root-secret}', gen_salt('bf', 8)),
      rootEnhet_Id
    );
  END IF;
END
$$
