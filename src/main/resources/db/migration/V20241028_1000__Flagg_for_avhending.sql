ALTER TABLE saksmappe
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();
CREATE INDEX IF NOT EXISTS saksmappe__accessible_after_reindexed_idx ON saksmappe ((_accessible_after - last_indexed));

ALTER TABLE journalpost
    ADD COLUMN IF NOT EXISTS avhendet_til__id text,
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW(),
    ADD CONSTRAINT journalpost_avhend_til_id_fkey FOREIGN KEY (avhendet_til__id) REFERENCES enhet(_id);
CREATE INDEX IF NOT EXISTS journalpost__accessible_after_reindexed_idx ON journalpost ((_accessible_after - last_indexed));

ALTER TABLE møtemappe
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();
CREATE INDEX IF NOT EXISTS motemappe__accessible_after_reindexed_idx ON møtemappe ((_accessible_after - last_indexed));

ALTER TABLE møtesaksregistrering
    ADD COLUMN IF NOT EXISTS avhendet_til__id text,
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW(),
    ADD CONSTRAINT motesak_avhend_til_id_fkey FOREIGN KEY (avhendet_til__id) REFERENCES enhet(_id);
CREATE INDEX IF NOT EXISTS motesaksregistrering__accessible_after_reindexed_idx ON møtesaksregistrering ((_accessible_after - last_indexed));

ALTER TABLE møtedokumentregistrering
    ADD COLUMN IF NOT EXISTS avhendet_til__id text,
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW(),
    ADD CONSTRAINT motedok_avhend_til_id_fkey FOREIGN KEY (avhendet_til__id) REFERENCES enhet(_id);

ALTER TABLE api_key
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE arkiv
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE arkivdel
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE behandlingsprotokoll
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE bruker
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE dokumentbeskrivelse
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE dokumentobjekt
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE enhet
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE identifikator
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE innsynskrav
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE innsynskrav_del
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE klasse
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE klassifikasjonssystem
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE korrespondansepart
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE lagret_sak
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE lagret_sok
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE lagret_sok_treff
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE lagret_soek_hit
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE skjerming
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE tilbakemelding
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE utredning
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE vedtak
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE votering
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE moetesaksbeskrivelse
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE moetedeltaker
    ADD COLUMN IF NOT EXISTS _accessible_after TIMESTAMPTZ DEFAULT NOW();
