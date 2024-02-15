CREATE TABLE IF NOT EXISTS møtemappe(
  _id TEXT DEFAULT einnsyn_id('mm')
);
ALTER TABLE IF EXISTS møtemappe
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('mm'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS utvalg__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS referanse_forrige_moete__id TEXT,
  ADD COLUMN IF NOT EXISTS referanse_neste_moete__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetemappe_id_idx ON møtemappe (_id);

CREATE TABLE IF NOT EXISTS møtesaksregistrering(
  _id TEXT DEFAULT einnsyn_id('ms')
);
ALTER TABLE IF EXISTS møtesaksregistrering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ms'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS last_indexed TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS utredning__id TEXT,
  ADD COLUMN IF NOT EXISTS innstilling__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtak__id TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ALTER COLUMN møtemappe_id DROP NOT NULL, /* Eases insertion from parent møtemappe */
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering_id_idx ON møtesaksregistrering (_id);

CREATE TABLE IF NOT EXISTS møtedokumentregistrering(
  _id TEXT DEFAULT einnsyn_id('md')
);
ALTER TABLE IF EXISTS møtedokumentregistrering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('md'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS administrativ_enhet__id TEXT,
  ADD COLUMN IF NOT EXISTS beskrivelse TEXT,
  ALTER COLUMN møtemappe_id DROP NOT NULL, /* Eases insertion from parent møtemappe */
  /* This is a legacy field, but the object should inherit from Registrering: */
  ADD COLUMN IF NOT EXISTS arkivskaper TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering_id_idx ON møtedokumentregistrering (_id);

/* Vedtak */
CREATE TABLE IF NOT EXISTS vedtak(
  _id TEXT DEFAULT einnsyn_id('ved')
);
ALTER TABLE IF EXISTS vedtak
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ved'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtakstekst__id TEXT,
  ADD COLUMN IF NOT EXISTS behandlingsprotokoll__id TEXT,
  ADD COLUMN IF NOT EXISTS dato DATE,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_id_idx ON vedtak (_id);

CREATE TABLE IF NOT EXISTS vedtak_vedtaksdokument(
  vedtak__id TEXT,
  vedtaksdokument__id TEXT,
  CONSTRAINT vedtak_vedtaksdokument_pkey PRIMARY KEY (vedtak__id, vedtaksdokument__id),
  CONSTRAINT vedtak_vedtaksdokument_vedtak__id_fkey FOREIGN KEY (vedtak__id) REFERENCES vedtak(_id),
  CONSTRAINT vedtak_vedtaksdokument_vedtaksdokument__id_fkey FOREIGN KEY (vedtaksdokument__id) REFERENCES dokumentbeskrivelse(_id)
);

/* Moetesaksbeskrivelse */
CREATE TABLE IF NOT EXISTS moetesaksbeskrivelse(
  _id TEXT DEFAULT einnsyn_id('msb')
);
ALTER TABLE IF EXISTS moetesaksbeskrivelse
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('msb'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS tekst_innhold TEXT,
  ADD COLUMN IF NOT EXISTS tekst_format TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_id_idx ON moetesaksbeskrivelse (_id);


/* Utredning */
CREATE TABLE IF NOT EXISTS utredning(
  _id TEXT DEFAULT einnsyn_id('utr')
);
ALTER TABLE IF EXISTS utredning
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('utr'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS saksbeskrivelse__id TEXT,
  ADD COLUMN IF NOT EXISTS innstilling__id TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS utredning_id_idx ON utredning (_id);

CREATE TABLE IF NOT EXISTS utredning_utredningsdokument(
  utredning__id TEXT,
  utredningsdokument__id TEXT,
  CONSTRAINT utredning_utredningsdokument_pkey PRIMARY KEY (utredning__id, utredningsdokument__id),
  CONSTRAINT utredning_utredningsdokument_utredning__id_fkey FOREIGN KEY (utredning__id) REFERENCES utredning(_id),
  CONSTRAINT utredning_utredningsdokument_utredningsdokument__id_fkey FOREIGN KEY (utredningsdokument__id) REFERENCES dokumentbeskrivelse(_id)
);


/* Behandlingsprotokoll */
CREATE TABLE IF NOT EXISTS behandlingsprotokoll(
  _id TEXT DEFAULT einnsyn_id('bp')
);
ALTER TABLE IF EXISTS behandlingsprotokoll
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('bp'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS tekst_innhold TEXT,
  ADD COLUMN IF NOT EXISTS tekst_format TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_id_idx ON behandlingsprotokoll (_id);


/* Votering */
CREATE TABLE IF NOT EXISTS votering(
  _id TEXT DEFAULT einnsyn_id('vot')
);
ALTER TABLE IF EXISTS votering
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('vot'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker__id TEXT,
  ADD COLUMN IF NOT EXISTS representerer__id TEXT,
  ADD COLUMN IF NOT EXISTS vedtak__id TEXT,
  ADD COLUMN IF NOT EXISTS stemme TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS votering_id_idx ON votering (_id);


/* Moetedeltaker */
CREATE TABLE IF NOT EXISTS moetedeltaker(
  _id TEXT DEFAULT einnsyn_id('md')
);
ALTER TABLE IF EXISTS moetedeltaker
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('md'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker_navn TEXT,
  ADD COLUMN IF NOT EXISTS moetedeltaker_funksjon TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_id_idx ON moetedeltaker (_id);


/* Identifikator */
CREATE TABLE IF NOT EXISTS identifikator(
  _id TEXT DEFAULT einnsyn_id('ide')
);
ALTER TABLE IF EXISTS identifikator
  ADD COLUMN IF NOT EXISTS _id TEXT DEFAULT einnsyn_id('ide'),
  ADD COLUMN IF NOT EXISTS _external_id TEXT,
  ADD COLUMN IF NOT EXISTS _created TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS _updated TIMESTAMPTZ DEFAULT now(),
  ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS system_id TEXT,
  ADD COLUMN IF NOT EXISTS journalenhet__id TEXT,
  ADD COLUMN IF NOT EXISTS navn TEXT,
  ADD COLUMN IF NOT EXISTS identifikator TEXT,
  ADD COLUMN IF NOT EXISTS initialer TEXT,
  ADD COLUMN IF NOT EXISTS epostadresse TEXT,
  /* This is a legacy field, but the object should inherit from ArkivBase: */
  ADD COLUMN IF NOT EXISTS virksomhet_iri TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_id_idx ON identifikator (_id);