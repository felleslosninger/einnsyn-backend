ALTER TABLE arkiv
  add column if not exists lock_version int default 0;

ALTER TABLE arkivdel
  add column if not exists lock_version int default 0;

ALTER TABLE klasse
  add column if not exists lock_version int default 0;

ALTER TABLE saksmappe
  add column if not exists lock_version int default 0;

ALTER TABLE møtemappe
  add column if not exists lock_version int default 0;

ALTER TABLE skjerming
  add column if not exists lock_version int default 0;

ALTER TABLE journalpost
  add column if not exists lock_version int default 0;

ALTER TABLE korrespondansepart
  add column if not exists lock_version int default 0;

ALTER TABLE møtesaksregistrering
  add column if not exists lock_version int default 0;

ALTER TABLE møtedokumentregistrering
  add column if not exists lock_version int default 0;

ALTER TABLE dokumentbeskrivelse
  add column if not exists lock_version int default 0;

ALTER TABLE dokumentobjekt
  add column if not exists lock_version int default 0;
