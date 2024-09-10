DROP INDEX IF EXISTS korrespondansepart__external_id_idx;

CREATE INDEX IF NOT EXISTS korrpart__external_nonunique_id_idx
    on korrespondansepart(_external_id);
