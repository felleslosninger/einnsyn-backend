-- Journalpost
CREATE INDEX journalpost_last_indexed_idx ON journalpost (last_indexed);
CREATE INDEX journalpost_last_indexed_stale_partial_idx ON journalpost (last_indexed, _updated)
	WHERE last_indexed < _updated;
CREATE INDEX journalpost_last_indexed_null_partial_idx ON journalpost (last_indexed)
	WHERE last_indexed IS NULL;

-- Saksmappe
CREATE INDEX saksmappe_last_indexed_idx ON saksmappe (last_indexed);
CREATE INDEX saksmappe_last_indexed_stale_partial_idx ON saksmappe (last_indexed, _updated)
  WHERE last_indexed < _updated;
CREATE INDEX saksmappe_last_indexed_null_partial_idx ON saksmappe (last_indexed)
  WHERE last_indexed IS NULL;

--Moetemappe
CREATE INDEX moetemappe_last_indexed_idx ON møtemappe (last_indexed);
CREATE INDEX moetemappe_last_indexed_stale_partial_idx ON møtemappe (last_indexed, _updated)
  WHERE last_indexed < _updated;
CREATE INDEX moetemappe_last_indexed_null_partial_idx ON møtemappe (last_indexed)
  WHERE last_indexed IS NULL;

-- Moetesak
CREATE INDEX moetesak_last_indexed_idx ON møtesaksregistrering (last_indexed);
CREATE INDEX moetesak_last_indexed_stale_partial_idx ON møtesaksregistrering (last_indexed, _updated)
  WHERE last_indexed < _updated;
CREATE INDEX moetesak_last_indexed_null_partial_idx ON møtesaksregistrering (last_indexed)
  WHERE last_indexed IS NULL;
