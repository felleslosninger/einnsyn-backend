create table if not exists bruker
(
    id             uuid                     not null
        constraint idx_16385_primary
            primary key,
    active         boolean                  not null,
    brukernavn     varchar(255)             not null,
    epost          varchar(255),
    etternavn      varchar(255),
    fornavn        varchar(255),
    login_forsok   bigint                   not null,
    oppdatert_dato timestamp with time zone not null,
    opprettet_dato timestamp with time zone not null,
    organisasjon   varchar(255),
    passord        varchar(255),
    passord_expiry timestamp with time zone,
    personnummer   varchar(255),
    secret         varchar(255),
    secret_expiry  timestamp with time zone,
    telefon        varchar(255),
    type           varchar(255)             not null,
    virksomhet     varchar(255)
);

create index if not exists idx_16385_idx5gnrkqrxikrxs2fsfuxtm54uy
    on bruker (type);

create unique index if not exists idx_16385_uk_20mfiinekea6fut2yv7xd1i3h
    on bruker (secret);

create index if not exists idx_16385_idxhre3okjcqnh9ede7rdp0u0agd
    on bruker (type, virksomhet);

create unique index if not exists idx_16385_uk_l9pslrpsos6j6en6rdjjdpxei
    on bruker (epost);

create unique index if not exists idx_16385_uk_ke1igvqb9n5lqf167jm0s7nmo
    on bruker (personnummer);

create unique index if not exists idx_16385_uk_iq3ud20qcvvwpe4s27pc1qday
    on bruker (brukernavn);

create table if not exists driftsmelding
(
    id             uuid                     not null
        constraint idx_16391_primary
            primary key,
    gyldig_fra     timestamp with time zone,
    gyldig_til     timestamp with time zone,
    innhold        text,
    oppdatert_dato timestamp with time zone not null,
    opprettet_dato timestamp with time zone not null,
    skjult         boolean                  not null
);

create index if not exists idx_16391_idx5p7u22qgcvimswgh5yn46nrkq
    on driftsmelding (gyldig_fra);

create index if not exists idx_16391_idxf00iykffwk6goaexsqit1gc3c
    on driftsmelding (gyldig_til);

create table if not exists frontend_states
(
    id            uuid    not null
        constraint idx_16403_primary
            primary key,
    expires_at    date,
    no_expiration boolean not null,
    state         bytea
);

create index if not exists idx_16403_idx_expires_at
    on frontend_states (expires_at);

create table if not exists innsynskrav
(
    id             uuid                     not null
        constraint idx_16409_primary
            primary key,
    bruker_iri     varchar(255),
    epost          varchar(255)             not null,
    opprettet_dato timestamp with time zone not null
);

create index if not exists idx_16409_idxm503ca14lmplb6xqo7x06unh0
    on innsynskrav (opprettet_dato);

create index if not exists idx_16409_idxgqkc9qypnxvuh1cm7y1ebdpk3
    on innsynskrav (bruker_iri);

create table if not exists innsynskrav_del
(
    id             uuid         not null
        constraint idx_16415_primary
            primary key,
    rettet_mot     varchar(255) not null,
    skjult         boolean      not null,
    virksomhet     varchar(255),
    innsynskrav_id uuid         not null
        constraint fkpugfjorvo1lw3q2v15v24d99g
            references innsynskrav
            on update restrict on delete restrict
);

create index if not exists idx_16415_idx2uuav3dndf0nb92lhdswma6yn
    on innsynskrav_del (skjult);

create index if not exists idx_16415_fkpugfjorvo1lw3q2v15v24d99g
    on innsynskrav_del (innsynskrav_id);

create table if not exists innsynskrav_del_status
(
    innsynskrav_del_id uuid not null
        constraint fk625v2c7uxdln3ge2rphlcatum
            references innsynskrav_del
            on update restrict on delete restrict,
    opprettet_dato     timestamp with time zone,
    status             varchar(255),
    systemgenerert     boolean
);

create index if not exists idx_16421_fk625v2c7uxdln3ge2rphlcatum
    on innsynskrav_del_status (innsynskrav_del_id);

create index if not exists idx_16421_idxpjbl8f2pn0tl2lvnsgtmb63qw
    on innsynskrav_del_status (status);

create table if not exists kategori
(
    id               uuid         not null
        constraint idx_16427_primary
            primary key,
    kategori         varchar(255) not null,
    kategori_engelsk varchar(255),
    kategori_nynorsk varchar(255),
    kategori_sami    varchar(255)
);

create table if not exists enhet
(
    id                   uuid                     not null
        constraint idx_16397_primary
            primary key,
    avsluttet_dato       timestamp with time zone,
    e_formidling         boolean                  not null,
    enhets_kode          varchar(255),
    innsynskrav_epost    varchar(255),
    iri                  varchar(255)             not null,
    kontaktpunkt_adresse varchar(255),
    kontaktpunkt_epost   varchar(255),
    kontaktpunkt_telefon varchar(255),
    navn                 varchar(255)             not null,
    navn_engelsk         varchar(255),
    navn_nynorsk         varchar(255),
    navn_sami            varchar(255),
    oppdatert_dato       timestamp with time zone not null,
    opprettet_dato       timestamp with time zone not null,
    orgnummer            varchar(255),
    skjult               boolean                  not null,
    type                 varchar(255)             not null,
    handteres_av_id      uuid
        constraint enhet_handteres_av_id_fkey
            references enhet
            on update restrict on delete restrict,
    kategori_id          uuid
        constraint fkoura2u78ppot6teskxo8h649
            references kategori
            on update restrict on delete restrict,
    parent_id            uuid
        constraint fkhj0x58rnue462adcqhl5jv86o
            references enhet
            on update restrict on delete restrict,
    vis_toppnode         boolean,
    skal_konvertere_id   boolean,
    er_teknisk           boolean
);

create index if not exists idx_16397_fk183qj589jx7sn7oxeoi4bchwe
    on enhet (handteres_av_id);

create unique index if not exists idx_16397_uk_gaub79tvikaukdbtghobj6eqs
    on enhet (iri);

create index if not exists idx_16397_fkhj0x58rnue462adcqhl5jv86o
    on enhet (parent_id);

create index if not exists idx_16397_fkoura2u78ppot6teskxo8h649
    on enhet (kategori_id);

create unique index if not exists idx_16397_uk_92705khprufmmir61ienjxj4d
    on enhet (orgnummer);

create table if not exists kategori_kategori_gyldig_for
(
    kategori_id            uuid not null
        constraint fkarorw49iqq97nlscwfumshg1d
            references kategori
            on update restrict on delete restrict,
    kategori_gyldig_for_id uuid not null
        constraint fkgtwgjls1kcuxu426cgl7uikxs
            references enhet
            on update restrict on delete restrict
);

create index if not exists idx_16433_fkgtwgjls1kcuxu426cgl7uikxs
    on kategori_kategori_gyldig_for (kategori_gyldig_for_id);

create index if not exists idx_16433_fkarorw49iqq97nlscwfumshg1d
    on kategori_kategori_gyldig_for (kategori_id);

create table if not exists lagret_sak
(
    id        uuid                     not null
        constraint idx_16439_primary
            primary key,
    opprettet timestamp with time zone not null,
    sak_id    varchar(255)             not null,
    bruker_id uuid                     not null
        constraint fkogti4for3qlgjxlucecfwhlw
            references bruker
            on update restrict on delete restrict
);

create index if not exists idx_16439_idxbxnbkp04ou9yc5hwiatguttfv
    on lagret_sak (sak_id);

create unique index if not exists idx_16439_uk6d781heacs6j5r5bl1oounu1p
    on lagret_sak (bruker_id, sak_id);

create table if not exists lagret_sok
(
    id             uuid                     not null
        constraint idx_16445_primary
            primary key,
    abonnere       boolean                  not null,
    label          varchar(255)             not null,
    opprettet_dato timestamp with time zone not null,
    sporring       text                     not null,
    sporring_es    text                     not null,
    bruker_id      uuid                     not null
        constraint fkg43wnecl89jmnkkc90mjbr2gu
            references bruker
            on update restrict on delete restrict
);

create index if not exists idx_16445_fkg43wnecl89jmnkkc90mjbr2gu
    on lagret_sok (bruker_id);

create table if not exists lagret_sok_treff
(
    id                   uuid                     not null
        constraint idx_16451_primary
            primary key,
    meldt_til_bruker_per varchar(255)             not null,
    opprettet            timestamp with time zone not null,
    rettet_mot           varchar(255)             not null,
    lagret_sok_id        uuid                     not null
        constraint fkqwo9kjgrh1500ykm9f70daird
            references lagret_sok
            on update restrict on delete restrict
);

create index if not exists idx_16451_fkqwo9kjgrh1500ykm9f70daird
    on lagret_sok_treff (lagret_sok_id);
alter table IF EXISTS enhet
    alter column skal_konvertere_id set default false;
alter table IF EXISTS enhet
    alter column vis_toppnode set default false;
alter table IF EXISTS enhet
    alter column er_teknisk set default false;
ALTER TABLE IF EXISTS enhet ADD COLUMN if not exists skal_motta_kvittering boolean DEFAULT false;
ALTER TABLE IF EXISTS enhet ADD COLUMN if not exists order_xml_versjon int;

UPDATE enhet
SET order_xml_versjon = 1
WHERE orgnummer IS NOT NULL;
ALTER TABLE IF EXISTS enhet ALTER COLUMN avsluttet_dato TYPE date;
ALTER TABLE IF EXISTS bruker ALTER COLUMN login_forsok TYPE int;
ALTER TABLE IF EXISTS lagret_sok DROP COLUMN IF EXISTS abonnere;
ALTER TABLE IF EXISTS innsynskrav ADD COLUMN if not exists verification_secret text;
ALTER TABLE IF EXISTS innsynskrav ADD COLUMN if not exists verified boolean;
ALTER TABLE IF EXISTS innsynskrav ADD COLUMN IF NOT EXISTS sendt_til_virksomhet timestamp with time zone;
