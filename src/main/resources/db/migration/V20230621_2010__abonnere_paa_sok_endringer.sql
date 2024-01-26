ALTER TABLE IF EXISTS lagret_sok_treff ALTER COLUMN meldt_til_bruker_per DROP NOT NULL;
ALTER TABLE IF EXISTS lagret_sok_treff ADD COLUMN IF NOT EXISTS meldt_til_bruker_tid timestamp with time zone;
ALTER TABLE IF EXISTS lagret_sok ADD COLUMN IF NOT EXISTS abonnere boolean NOT NULL DEFAULT false;
ALTER TABLE IF EXISTS lagret_sok ADD COLUMN IF NOT EXISTS oppdatert timestamp with time zone;
ALTER TABLE IF EXISTS lagret_sok_treff ADD COLUMN IF NOT EXISTS rettet_mot_type text;
ALTER TABLE IF EXISTS lagret_sak ADD COLUMN IF NOT EXISTS abonnere boolean NOT NULL DEFAULT false;

ALTER TABLE IF EXISTS lagret_sak ADD COLUMN IF NOT EXISTS oppdatert timestamp with time zone;

create table if not exists lagret_sak_treff
(
    id                   uuid                     not null
        constraint lagret_sak_treff_pk_idx
            primary key,
    meldt_til_bruker_per text,
    meldt_til_bruker_tid timestamp with time zone,
    opprettet            timestamp with time zone not null,
    rettet_mot           text                     not null,
    rettet_mot_type      text                     not null,
    lagret_sak_id        uuid                     not null
        constraint lagret_sak_treff_lagret_sak_idx
            references lagret_sak
            on update restrict on delete restrict
);

create index if not exists lagret_sak_treff_lagret_sak_idx
    on lagret_sak_treff (lagret_sak_id);
ALTER TABLE IF EXISTS lagret_sak_treff ADD COLUMN IF NOT EXISTS rettet_mot_tittel text;
ALTER TABLE IF EXISTS lagret_sok_treff ADD COLUMN IF NOT EXISTS rettet_mot_tittel text;