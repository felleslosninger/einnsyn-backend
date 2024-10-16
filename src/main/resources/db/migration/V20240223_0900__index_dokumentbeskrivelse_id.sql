CREATE INDEX IF NOT EXISTS journalpost_dokbesk_dok_id_idx
    ON journalpost_dokumentbeskrivelse (dokumentbeskrivelse_id);

CREATE INDEX IF NOT EXISTS moetesak_dokbesk_dok_id_idx
    ON møtesaksregistrering_dokumentbeskrivelse (dokumentbeskrivelse_id);

CREATE INDEX IF NOT EXISTS moetedok_dokbesk_dok_id_idx
    ON møtedokumentregistrering_dokumentbeskrivelse (dokumentbeskrivelse_id);
