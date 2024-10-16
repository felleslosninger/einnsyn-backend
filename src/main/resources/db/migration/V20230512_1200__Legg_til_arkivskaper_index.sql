CREATE INDEX IF NOT EXISTS møtemappe_arkivskaper_idx
  ON møtemappe (arkivskaper);

CREATE INDEX IF NOT EXISTS saksmappe_arkivskaper_idx
  ON saksmappe (arkivskaper);

CREATE INDEX IF NOT EXISTS journalpost_arkivskaper_idx
  ON journalpost (arkivskaper);

CREATE INDEX IF NOT EXISTS møtesaksregistrering_arkivskaper_idx
  ON møtesaksregistrering (arkivskaper);
