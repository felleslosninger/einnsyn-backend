ALTER TABLE saksmappe
    ADD COLUMN If NOT EXISTS avhendet_til text,
    ADD COLUMN If NOT EXISTS _hidden boolean default false,
    ADD CONSTRAINT saksmappe_avhend_til_id_fkey FOREIGN KEY (avhendet_til) REFERENCES enhet(_id);

ALTER TABLE journalpost
    ADD COLUMN If NOT EXISTS avhendet_til text,
    ADD COLUMN If NOT EXISTS _hidden boolean default false,
    ADD CONSTRAINT journalpost_avhend_til_id_fkey FOREIGN KEY (avhendet_til) REFERENCES enhet(_id);

ALTER TABLE møtemappe
    ADD COLUMN If NOT EXISTS avhendet_til text,
    ADD COLUMN If NOT EXISTS _hidden boolean default false,
    ADD CONSTRAINT motemappe_avhend_til_id_fkey FOREIGN KEY (avhendet_til) REFERENCES enhet(_id);

ALTER TABLE møtesaksregistrering
    ADD COLUMN If NOT EXISTS avhendet_til text,
    ADD COLUMN If NOT EXISTS _hidden boolean default false,
    ADD CONSTRAINT motesak_avhend_til_id_fkey FOREIGN KEY (avhendet_til) REFERENCES enhet(_id);

ALTER TABLE møtedokumentregistrering
    ADD COLUMN If NOT EXISTS avhendet_til text,
    ADD COLUMN If NOT EXISTS _hidden boolean default false,
    ADD CONSTRAINT motedok_avhend_til_id_fkey FOREIGN KEY (avhendet_til) REFERENCES enhet(_id);

ALTER TABLE api_key
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE arkiv
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE arkivdel
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE behandlingsprotokoll
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE bruker
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE dokumentbeskrivelse
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE dokumentobjekt
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE enhet
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE identifikator
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE innsynskrav
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE innsynskrav_del
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE klasse
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE klassifikasjonssystem
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE korrespondansepart
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE lagret_sak
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE lagret_sok
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE lagret_sok_treff
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE lagret_soek_hit
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE skjerming
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE tilbakemelding
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE utredning
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE vedtak
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE votering
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE moetesaksbeskrivelse
    ADD COLUMN If NOT EXISTS _hidden boolean default false;

ALTER TABLE moetedeltaker
    ADD COLUMN If NOT EXISTS _hidden boolean default false;
