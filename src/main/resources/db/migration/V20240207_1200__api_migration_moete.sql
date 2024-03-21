/* Moetemappe */
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
CREATE UNIQUE INDEX IF NOT EXISTS moetemappe__external_id_idx ON møtemappe (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetemappe_system_id_idx ON møtemappe (system_id);
CREATE INDEX IF NOT EXISTS moetemappe_system_id_nonunique_idx ON møtemappe (system_id);
CREATE INDEX IF NOT EXISTS moetemappe_created_idx ON møtemappe (_created);
CREATE INDEX IF NOT EXISTS moetemappe_updated_idx ON møtemappe (_updated);
CREATE INDEX IF NOT EXISTS moetemappe_journalenhet__id ON møtemappe(journalenhet__id);


/* Moetesaksregistrering */
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
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering__external_id_idx ON møtesaksregistrering (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetesaksregistrering_system_id_idx ON møtesaksregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_system_id_nonunique_idx ON møtesaksregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_created_idx ON møtesaksregistrering (_created);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_updated_idx ON møtesaksregistrering (_updated);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_journalenhet__id ON møtesaksregistrering(journalenhet__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_administrativ_enhet__id ON møtesaksregistrering(administrativ_enhet__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_utredning__id ON møtesaksregistrering(utredning__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_innstilling__id ON møtesaksregistrering(innstilling__id);
CREATE INDEX IF NOT EXISTS moetesaksregistrering_vedtak__id ON møtesaksregistrering(vedtak__id);


/* Møtedokumentregistrering */
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
CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering__external_id_idx ON møtedokumentregistrering (_external_id);
--CREATE UNIQUE INDEX IF NOT EXISTS moetemøtedokumentregistrering_system_id_idx ON møtedokumentregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_system_id_nonunique_idx ON møtedokumentregistrering (system_id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_created_idx ON møtedokumentregistrering (_created);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_updated_idx ON møtedokumentregistrering (_updated);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_journalenhet__id ON møtedokumentregistrering(journalenhet__id);
CREATE INDEX IF NOT EXISTS moetemøtedokumentregistrering_administrativ_enhet__id ON møtedokumentregistrering(administrativ_enhet__id);


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
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_external_id_idx ON vedtak (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS vedtak_system_id_idx ON vedtak (system_id);
CREATE INDEX IF NOT EXISTS vedtak_created_idx ON vedtak (_created);
CREATE INDEX IF NOT EXISTS vedtak_updated_idx ON vedtak (_updated);
CREATE INDEX IF NOT EXISTS vedtak_journalenhet__id ON vedtak(journalenhet__id);
CREATE INDEX IF NOT EXISTS vedtak_vedtakstekst__id ON vedtak(vedtakstekst__id);
CREATE INDEX IF NOT EXISTS vedtak_behandlingsprotokoll__id ON vedtak(behandlingsprotokoll__id);

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
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_external_id_idx ON moetesaksbeskrivelse (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetesaksbeskrivelse_system_id_idx ON moetesaksbeskrivelse (system_id);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_created_idx ON moetesaksbeskrivelse (_created);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_updated_idx ON moetesaksbeskrivelse (_updated);
CREATE INDEX IF NOT EXISTS moetesaksbeskrivelse_journalenhet__id ON moetesaksbeskrivelse(journalenhet__id);


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
CREATE UNIQUE INDEX IF NOT EXISTS utredning_external_id_idx ON utredning (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS utredning_system_id_idx ON utredning (system_id);
CREATE INDEX IF NOT EXISTS utredning_created_idx ON utredning (_created);
CREATE INDEX IF NOT EXISTS utredning_updated_idx ON utredning (_updated);
CREATE INDEX IF NOT EXISTS utredning_journalenhet__id ON utredning(journalenhet__id);
CREATE INDEX IF NOT EXISTS utredning_saksbeskrivelse__id ON utredning(saksbeskrivelse__id);
CREATE INDEX IF NOT EXISTS utredning_innstilling__id ON utredning(innstilling__id);

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
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_external_id_idx ON behandlingsprotokoll (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS behandlingsprotokoll_system_id_idx ON behandlingsprotokoll (system_id);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_created_idx ON behandlingsprotokoll (_created);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_updated_idx ON behandlingsprotokoll (_updated);
CREATE INDEX IF NOT EXISTS behandlingsprotokoll_journalenhet__id ON behandlingsprotokoll(journalenhet__id);


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
CREATE UNIQUE INDEX IF NOT EXISTS votering_external_id_idx ON votering (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS votering_system_id_idx ON votering (system_id);
CREATE INDEX IF NOT EXISTS votering_created_idx ON votering (_created);
CREATE INDEX IF NOT EXISTS votering_updated_idx ON votering (_updated);
CREATE INDEX IF NOT EXISTS votering_journalenhet__id ON votering(journalenhet__id);
CREATE INDEX IF NOT EXISTS votering_moetedeltaker__id ON votering(moetedeltaker__id);
CREATE INDEX IF NOT EXISTS votering_representerer__id ON votering(representerer__id);
CREATE INDEX IF NOT EXISTS votering_vedtak__id ON votering(vedtak__id);


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
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_external_id_idx ON moetedeltaker (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS moetedeltaker_system_id_idx ON moetedeltaker (system_id);
CREATE INDEX IF NOT EXISTS moetedeltaker_created_idx ON moetedeltaker (_created);
CREATE INDEX IF NOT EXISTS moetedeltaker_updated_idx ON moetedeltaker (_updated);
CREATE INDEX IF NOT EXISTS moetedeltaker_journalenhet__id ON moetedeltaker(journalenhet__id);


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
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_external_id_idx ON identifikator (_external_id);
CREATE UNIQUE INDEX IF NOT EXISTS identifikator_system_id_idx ON identifikator (system_id);
CREATE INDEX IF NOT EXISTS identifikator_created_idx ON identifikator (_created);
CREATE INDEX IF NOT EXISTS identifikator_updated_idx ON identifikator (_updated);
CREATE INDEX IF NOT EXISTS identifikator_journalenhet__id ON identifikator(journalenhet__id);
