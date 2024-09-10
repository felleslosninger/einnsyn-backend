DROP INDEX IF EXISTS korrespondansepart__external_id_idx;

CREATE INDEX IF NOT EXISTS korrespondansepart__external_id_nonunique_idx
    on korrespondansepart(_external_id);
