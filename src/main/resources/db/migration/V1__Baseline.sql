CREATE SEQUENCE IF NOT EXISTS arkiv_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS arkiv(
    arkiv_id int PRIMARY KEY
      default nextval('arkiv_seq'),
    arkiv_iri text,
    virksomhet_iri text,
    parentarkiv_id int,
    -- parentarkiv_iri er brukt under migrering for å koble ID, utelatt i jpa-impl
    parentarkiv_iri text,
    system_id text,
    tittel text,
    publisert_dato timestamptz,
    UNIQUE (arkiv_iri, virksomhet_iri),
    CONSTRAINT arkiv_parentarkiv_id_fkey FOREIGN KEY (parentarkiv_id) REFERENCES arkiv(arkiv_id)
);

CREATE SEQUENCE IF NOT EXISTS arkivdel_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS arkivdel(
    arkivdel_id int PRIMARY KEY
      default nextval('arkivdel_seq'),
    arkivdel_iri text,
    virksomhet_iri text,
    arkiv_id int,
    -- arkiv_iri brukes under migrering, utelatt i jpa-impl
    arkiv_iri text,
    system_id text,
    tittel text,
    publisert_dato timestamptz,
    UNIQUE (arkivdel_iri, virksomhet_iri),
    CONSTRAINT arkivdel_arkiv_id_fkey FOREIGN KEY (arkiv_id) REFERENCES arkiv(arkiv_id)
);

CREATE SEQUENCE IF NOT EXISTS klasse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS klasse(
    klasse_id int PRIMARY KEY
      default nextval('klasse_seq'),
    klasse_iri text,
    virksomhet_iri text,
    arkivdel_id int,
    parentklasse int,
    -- arkivdel_iri brukes under migrering, utelatt i jpa-impl
    arkivdel_iri text,
    klasse_id_string text,
    system_id text,
    tittel text,
    nøkkelord text,
    UNIQUE (klasse_iri, virksomhet_iri),
    CONSTRAINT klasse_arkivdel_id_fkey FOREIGN KEY (arkivdel_id) REFERENCES arkivdel(arkivdel_id),
    CONSTRAINT klasse_parentklasse_fkey FOREIGN KEY (parentklasse) REFERENCES klasse(klasse_id)
);

CREATE SEQUENCE IF NOT EXISTS saksmappe_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS saksmappe(
    saksmappe_id int PRIMARY KEY
      default nextval('saksmappe_seq'),
    saksmappe_iri text,
    arkiv_id int,
    arkivdel_id int,
    klasse_id int,
    -- parent_iri brukes under migrering, er utelatt i jpa-impl
    parent_iri text,
    virksomhet_iri text,
    ekstern_id text,
    mappe_id text,
    system_id text,
    tittel text,
    offentlig_tittel text,
    tittel_sensitiv text,
    offentlig_tittel_sensitiv text,
    beskrivelse text,
    saksaar int,
    sakssekvensnummer int,
    saksdato date,
    administrativ_enhet text,
    arkivskaper text,
    journalenhet text,
    oppdatert_dato timestamptz,
    publisert_dato timestamptz,
    saksansvarlig text,
    UNIQUE (saksmappe_iri, virksomhet_iri),
    CONSTRAINT saksmappe_arkiv_id_fkey FOREIGN KEY (arkiv_id) REFERENCES arkiv(arkiv_id),
    CONSTRAINT saksmappe_arkivdel_id_fkey FOREIGN KEY (arkivdel_id) REFERENCES arkivdel(arkivdel_id),
    CONSTRAINT saksmappe_klasse_id_fkey FOREIGN KEY (klasse_id) REFERENCES klasse(klasse_id)
);

CREATE SEQUENCE IF NOT EXISTS møtemappe_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS møtemappe(
    møtemappe_id int PRIMARY KEY
      default nextval('møtemappe_seq'),
    møtemappe_iri text,
    arkiv_id int,
    arkivdel_id int,
    klasse_id int,
    -- parent_iri brukes under migrering, er utelatt i jpa-impl
    parent_iri text,
    virksomhet_iri text,
    system_id text,
    tittel text,
    offentlig_tittel text,
    tittel_sensitiv text,
    offentlig_tittel_sensitiv text,
    beskrivelse text,
    møtenummer text,
    utvalg text,
    møtedato timestamptz,
    møtested text,
    videolink text,
    arkivskaper text,
    mappe_id text,
    oppdatert_dato timestamptz,
    publisert_dato timestamptz,
    UNIQUE (møtemappe_iri, virksomhet_iri),
    CONSTRAINT møtemappe_arkiv_id_fkey FOREIGN KEY (arkiv_id) REFERENCES arkiv(arkiv_id),
    CONSTRAINT møtemappe_arkivdel_id_fkey FOREIGN KEY (arkivdel_id) REFERENCES arkivdel(arkivdel_id),
    CONSTRAINT møtemappe_klasse_id_fkey FOREIGN KEY (klasse_id) REFERENCES klasse(klasse_id)
);

CREATE SEQUENCE IF NOT EXISTS skjerming_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS skjerming(
    skjerming_id int PRIMARY KEY
      default nextval('skjerming_seq'),
    skjerming_iri text,
    tilgangsrestriksjon text,
    skjermingshjemmel text
);

CREATE SEQUENCE IF NOT EXISTS journalpost_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS journalpost(
    journalpost_id int PRIMARY KEY
      default nextval('journalpost_seq'),
    journalpost_iri text,
    saksmappe_id int,
    skjerming_id int,
    -- saksmappe_iri og skjerming_iri brukes i migrering, utelates i jpa-impl
    saksmappe_iri text,
    virksomhet_iri text,
    skjerming_iri text,
    ekstern_id text,
    system_id text,
    tittel text,
    offentlig_tittel text,
    tittel_sensitiv text,
    offentlig_tittel_sensitiv text,
    journalaar int,
    journalsekvensnummer int,
    journalpostnummer int,
    journalposttype text,
    journaldato date,
    dokumentdato date,
    journalenhet text,
    arkivskaper text,
    oppdatert_dato timestamptz,
    publisert_dato timestamptz,
    registreringsid text,
    UNIQUE (journalpost_iri, virksomhet_iri),
    CONSTRAINT journalpost_saksmappe_id_fkey FOREIGN KEY (saksmappe_id) REFERENCES saksmappe(saksmappe_id),
    CONSTRAINT journalpost_skjerming_id_fkey FOREIGN KEY (skjerming_id) REFERENCES skjerming(skjerming_id)

);

CREATE SEQUENCE IF NOT EXISTS journalpost_følgsakenreferanse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS journalpost_følgsakenreferanse(
    journalpost_følgsakenreferanse_id int
        default nextval('journalpost_følgsakenreferanse_seq'),
    journalpost_fra_id int,
    -- migrering klarer å koble id for til_iri -> til_id
    journalpost_til_id int,
    journalpost_fra_iri text,
    journalpost_til_iri text,
    CONSTRAINT journalpost_følgsakenreferanse_journalpost_fra_id_fkey FOREIGN KEY (journalpost_fra_id) REFERENCES journalpost(journalpost_id),
    CONSTRAINT journalpost_følgsakenreferanse_journalpost_til_id_fkey FOREIGN KEY (journalpost_til_id) REFERENCES journalpost(journalpost_id)
);


-- merk at migreringen har med en koblingstabell mellom journalpost og korr.part
CREATE SEQUENCE IF NOT EXISTS korrespondansepart_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS korrespondansepart(
    korrespondansepart_id int PRIMARY KEY
      default nextval('korrespondansepart_seq'),
    korrespondansepart_iri text,
    journalpost_id int,
    korrespondanseparttype text,
    korrespondansepart_navn text,
    korrespondansepart_navn_sensitiv text,
    administrativ_enhet text,
    saksbehandler text,
    epostadresse text,
    postnummer text,
    CONSTRAINT korrespondansepart_journalpost_id_fkey FOREIGN KEY (journalpost_id) REFERENCES journalpost(journalpost_id)
);

CREATE SEQUENCE IF NOT EXISTS møtesaksregistrering_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS møtesaksregistrering(
    møtesaksregistrering_id int PRIMARY KEY
      default nextval('møtesaksregistrering_seq'),
    møtesaksregistrering_iri text,
    møtemappe_id int,
    -- migrering klarer å koble journalpost_iri og journalpost_id i en operasjon etter at alle data er på plass
    -- det er litt mer klønete i kjørende kode siden det ikke er sikkert journalposten fins (har id) enda
    journalpost_id int,
    -- møtemappe_iri brukes under migrering, utelatt i jpa-impl
    møtemappe_iri text,
    virksomhet_iri text,
    -- journalpost_iri == referanseTilMøtesak, bør navnet endres?
    journalpost_iri text,
    system_id text,
    tittel text,
    offentlig_tittel text,
    tittel_sensitiv text,
    offentlig_tittel_sensitiv text,
    møtesakstype text,
    administrativ_enhet text,
    møtesakssekvensnummer int,
    møtesaksår text,
    videolink text,
    saksbehandler text,
    saksbehandler_sensitiv text,
    arkivskaper text,
    oppdatert_dato timestamptz,
    publisert_dato timestamptz,
    registreringsid_var text,
    UNIQUE (møtesaksregistrering_iri, virksomhet_iri),
    CONSTRAINT møtesaksregistrering_møtemappe_id_fkey FOREIGN KEY (møtemappe_id) REFERENCES møtemappe(møtemappe_id),
    CONSTRAINT møtesaksregistrering_journalpost_id_fkey FOREIGN KEY (journalpost_id) REFERENCES journalpost(journalpost_id)
);

CREATE SEQUENCE IF NOT EXISTS møtedokumentregistrering_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS møtedokumentregistrering(
    møtedokumentregistrering_id int PRIMARY KEY
      default nextval('møtedokumentregistrering_seq'),
    møtedokumentregistrering_iri text,
    møtemappe_id int,
    -- møtemappe_iri brukes i migrering
    møtemappe_iri text,
    virksomhet_iri text,
    system_id text,
    tittel text,
    offentlig_tittel text,
    tittel_sensitiv text,
    offentlig_tittel_sensitiv text,
    møtedokumentregistreringstype text,
    administrativ_enhet text,
    saksbehandler text,
    saksbehandler_sensitiv text,
    oppdatert_dato timestamptz,
    publisert_dato timestamptz,
    UNIQUE (møtedokumentregistrering_iri, virksomhet_iri),
    CONSTRAINT møtedokumentregistrering_møtemappe_id_fkey FOREIGN KEY (møtemappe_id) REFERENCES møtemappe(møtemappe_id)
);

CREATE SEQUENCE IF NOT EXISTS dokumentbeskrivelse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS dokumentbeskrivelse(
    dokumentbeskrivelse_id int PRIMARY KEY
      default nextval('dokumentbeskrivelse_seq'),
    dokumentbeskrivelse_iri text,
    system_id text,
    dokumentnummer int,
    tilknyttet_registrering_som text,
    dokumenttype text,
    tittel text,
    tittel_sensitiv text
);

CREATE SEQUENCE IF NOT EXISTS møtedokumentregistrering_dokumentbeskrivelse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS møtedokumentregistrering_dokumentbeskrivelse(
    møtedokumentregistrering_dokumentbeskrivelse_id int
        default nextval('møtedokumentregistrering_dokumentbeskrivelse_seq')
        PRIMARY KEY,
    møtedokumentregistrering_id int,
    dokumentbeskrivelse_id int,
    CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_møtedokumentregistrering_id_fkey FOREIGN KEY (møtedokumentregistrering_id) REFERENCES møtedokumentregistrering(møtedokumentregistrering_id),
    CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id)
);

CREATE SEQUENCE IF NOT EXISTS møtesaksregistrering_dokumentbeskrivelse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS møtesaksregistrering_dokumentbeskrivelse(
    møtesaksregistrering_dokumentbeskrivelse_id int
        default nextval('møtesaksregistrering_dokumentbeskrivelse_seq')
        PRIMARY KEY,
    møtesaksregistrering_id int,
    dokumentbeskrivelse_id int,
    CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_møtesaksregistrering_id_fkey FOREIGN KEY (møtesaksregistrering_id) REFERENCES møtesaksregistrering (møtesaksregistrering_id),
    CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id)
);

CREATE SEQUENCE IF NOT EXISTS journalpost_dokumentbeskrivelse_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS journalpost_dokumentbeskrivelse(
    journalpost_dokumentbeskrivelse_id int
        default nextval('journalpost_dokumentbeskrivelse_seq')
        PRIMARY KEY,
    journalpost_id int,
    dokumentbeskrivelse_id int,
    CONSTRAINT journalpost_dokumentbeskrivelse_journalpost_id_fkey FOREIGN KEY (journalpost_id) REFERENCES journalpost(journalpost_id),
    CONSTRAINT journalpost_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id)
);

CREATE SEQUENCE IF NOT EXISTS dokumentobjekt_seq INCREMENT 1 START 1;
CREATE TABLE IF NOT EXISTS dokumentobjekt(
    dokumentobjekt_id int PRIMARY KEY
      default nextval('dokumentobjekt_seq'),
    dokumentobjekt_iri text,
    dokumentbeskrivelse_id int,
    -- dokumentbeskrivelse_iri brukes i migrering
    dokumentbeskrivelse_iri text,
    system_id text,
    referanse_dokumentfil text,
    dokument_format text,
    sjekksum text,
    sjekksumalgoritme text,
    formatdetaljer text,
    CONSTRAINT dokumentobjekt_dokumentbeskrivelse_id_fkey FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id)
);

CREATE INDEX saksmappe_ekstern_id_idx
    ON saksmappe (ekstern_id);
CREATE INDEX journalpost_saksmappe_id_idx
    ON journalpost (saksmappe_id);
CREATE INDEX journalpost_ekstern_id_idx
    ON journalpost (ekstern_id);
CREATE INDEX journalpost_skjerming_id_idx
    ON journalpost (skjerming_id);
CREATE INDEX møtesaksregistrering_møtemappe_id_idx
    ON møtesaksregistrering (møtemappe_id);
CREATE INDEX møtedokumentregistrering_møtemappe_id_idx
    ON møtedokumentregistrering (møtemappe_id);
CREATE INDEX møtedokumentregistrering_dokumentbeskrivelse_møtedokument_id
    ON møtedokumentregistrering_dokumentbeskrivelse (møtedokumentregistrering_id);
CREATE INDEX møtesaksregistrering_dokumentbeskrivelse_møtesak_id
    ON møtesaksregistrering_dokumentbeskrivelse (møtesaksregistrering_id);
CREATE INDEX journalpost_dokumentbeskrivelse_journalpost_id
    ON journalpost_dokumentbeskrivelse (journalpost_id);
CREATE INDEX dokumentobjekt_dokumentbeskrivelse_id
    ON dokumentobjekt (dokumentbeskrivelse_id);
CREATE INDEX møtesaksregistrering_journalpost_iri
    ON møtesaksregistrering (journalpost_iri);
CREATE INDEX korrespondansepart_journalpost_id
    ON korrespondansepart (journalpost_id);
CREATE INDEX dokumentobjekt_dokumentobjekt_iri_idx
    ON dokumentobjekt(dokumentobjekt_iri);
