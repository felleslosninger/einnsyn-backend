ALTER TABLE arkivdel
  ALTER COLUMN arkiv_id SET NOT NULL;

ALTER TABLE saksmappe
  ALTER COLUMN saksaar SET NOT NULL,
  ALTER COLUMN sakssekvensnummer SET NOT NULL;

ALTER TABLE møtemappe
  ALTER COLUMN utvalg SET NOT NULL;

ALTER TABLE skjerming
  ALTER COLUMN tilgangsrestriksjon SET NOT NULL;

ALTER TABLE journalpost
  ALTER COLUMN saksmappe_id SET NOT NULL,
  ALTER COLUMN journalaar SET NOT NULL,
  ALTER COLUMN journalposttype SET NOT NULL;

ALTER TABLE journalpost_følgsakenreferanse
  ALTER COLUMN journalpost_fra_id SET NOT NULL,
  ALTER COLUMN journalpost_til_iri SET NOT NULL;

ALTER TABLE korrespondansepart
  ALTER COLUMN journalpost_id SET NOT NULL,
  ALTER COLUMN korrespondanseparttype SET NOT NULL,
  ALTER COLUMN korrespondansepart_navn SET NOT NULL;

ALTER TABLE møtesaksregistrering
  ALTER COLUMN møtemappe_id SET NOT NULL,
  ALTER COLUMN møtesakstype SET NOT NULL,
  ALTER COLUMN møtesakssekvensnummer SET NOT NULL,
  ALTER COLUMN møtesaksår SET NOT NULL;

ALTER TABLE møtedokumentregistrering
  ALTER COLUMN møtemappe_id SET NOT NULL,
  ALTER COLUMN møtedokumentregistreringstype SET NOT NULL;

ALTER TABLE dokumentbeskrivelse
  ALTER COLUMN tilknyttet_registrering_som SET NOT NULL;

ALTER TABLE møtedokumentregistrering_dokumentbeskrivelse
  ALTER COLUMN møtedokumentregistrering_id SET NOT NULL,
  ALTER COLUMN dokumentbeskrivelse_id SET NOT NULL;

ALTER TABLE møtesaksregistrering_dokumentbeskrivelse
  ALTER COLUMN møtesaksregistrering_id SET NOT NULL,
  ALTER COLUMN dokumentbeskrivelse_id SET NOT NULL;

ALTER TABLE journalpost_dokumentbeskrivelse
  ALTER COLUMN journalpost_id SET NOT NULL,
  ALTER COLUMN dokumentbeskrivelse_id SET NOT NULL;

ALTER TABLE dokumentobjekt
  ALTER COLUMN dokumentbeskrivelse_id SET NOT NULL,
  ALTER COLUMN referanse_dokumentfil SET NOT NULL;



