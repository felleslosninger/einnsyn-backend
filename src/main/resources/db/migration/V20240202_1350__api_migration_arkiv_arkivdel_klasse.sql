CREATE TABLE IF NOT EXISTS arkiv(
  _id TEXT DEFAULT einnsyn_id('ark')
);
ALTER TABLE IF EXISTS arkiv
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ark'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS arkiv_id_idx ON arkiv (_id);

CREATE TABLE IF NOT EXISTS arkivdel(
  _id TEXT DEFAULT einnsyn_id('arkd')
);
ALTER TABLE IF EXISTS arkivdel
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('arkd'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS arkivdel_id_idx ON arkivdel (_id);

CREATE TABLE IF NOT EXISTS klasse(
  _id TEXT DEFAULT einnsyn_id('kla')
);
ALTER TABLE IF EXISTS klasse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('kla'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT,
  ADD COLUMN IF NOT EXISTS klassifikasjonssystem__id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS klasse_id_idx ON klasse (_id);
CREATE INDEX IF NOT EXISTS klasse_klassifikasjonssystem__id ON klasse(klassifikasjonssystem__id);

CREATE TABLE IF NOT EXISTS klassifikasjonssystem(
  _id TEXT DEFAULT einnsyn_id('ksys')
);
ALTER TABLE IF EXISTS klassifikasjonssystem
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ksys'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS lock_version INT DEFAULT 0,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT,
  ADD COLUMN IF NOT EXISTS arkivdel__id TEXT,
  ADD COLUMN IF NOT EXISTS tittel TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS klassifikasjonssystem_id_idx ON klassifikasjonssystem (_id);
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_arkivdel ON klassifikasjonssystem(arkivdel__id);

CREATE TABLE IF NOT EXISTS møtemappe(
  _id TEXT DEFAULT einnsyn_id('mm')
);
ALTER TABLE IF EXISTS møtemappe
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('mm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ;
CREATE UNIQUE INDEX IF NOT EXISTS møtemappe_id_idx ON møtemappe (_id);
