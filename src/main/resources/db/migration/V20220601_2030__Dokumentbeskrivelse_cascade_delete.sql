ALTER TABLE møtedokumentregistrering_dokumentbeskrivelse
    DROP CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey,
    DROP CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_møtedokumentregistrering_id_fkey,
    ADD CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey
        FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id) ON DELETE CASCADE,
    ADD CONSTRAINT møtedokumentregistrering_dokumentbeskrivelse_møtedokumentregistrering_id_fkey
        FOREIGN KEY (møtedokumentregistrering_id) REFERENCES møtedokumentregistrering(møtedokumentregistrering_id) ON DELETE CASCADE;

ALTER TABLE møtesaksregistrering_dokumentbeskrivelse
    DROP CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey,
    DROP CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_møtesaksregistrering_id_fkey,
    ADD CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey
        FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id) ON DELETE CASCADE,
    ADD CONSTRAINT møtesaksregistrering_dokumentbeskrivelse_møtesaksregistrering_id_fkey
        FOREIGN KEY (møtesaksregistrering_id) REFERENCES møtesaksregistrering (møtesaksregistrering_id) ON DELETE CASCADE;

ALTER TABLE journalpost_dokumentbeskrivelse
    DROP CONSTRAINT journalpost_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey,
    DROP CONSTRAINT journalpost_dokumentbeskrivelse_journalpost_id_fkey,
    ADD CONSTRAINT journalpost_dokumentbeskrivelse_dokumentbeskrivelse_id_fkey
        FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id) ON DELETE CASCADE,
    ADD CONSTRAINT journalpost_dokumentbeskrivelse_journalpost_id_fkey FOREIGN KEY (journalpost_id) REFERENCES journalpost(journalpost_id) ON DELETE CASCADE;

ALTER TABLE dokumentobjekt
    DROP CONSTRAINT dokumentobjekt_dokumentbeskrivelse_id_fkey,
    ADD CONSTRAINT dokumentobjekt_dokumentbeskrivelse_id_fkey
        FOREIGN KEY (dokumentbeskrivelse_id) REFERENCES dokumentbeskrivelse(dokumentbeskrivelse_id) ON DELETE CASCADE;
