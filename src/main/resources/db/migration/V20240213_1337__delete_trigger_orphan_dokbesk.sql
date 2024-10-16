CREATE OR REPLACE FUNCTION process_dokbeskrivelse() RETURNS TRIGGER AS
$body$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        delete from dokumentbeskrivelse dbe
        where dbe.dokumentbeskrivelse_id = OLD.dokumentbeskrivelse_id
          and not exists (select * from journalpost_dokumentbeskrivelse jdb
                          where jdb.dokumentbeskrivelse_id = OLD.dokumentbeskrivelse_id)
          and not exists (select * from møtesaksregistrering_dokumentbeskrivelse mdb
                          where mdb.dokumentbeskrivelse_id = OLD.dokumentbeskrivelse_id)
          and not exists (select * from møtedokumentregistrering_dokumentbeskrivelse mmdb
                          where mmdb.dokumentbeskrivelse_id = OLD.dokumentbeskrivelse_id);
    END IF;
    RETURN null;
END;
$body$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS delete_orphan_dokbesk_jp ON journalpost_dokumentbeskrivelse;
CREATE TRIGGER delete_orphan_dokbesk_jp
    AFTER DELETE ON journalpost_dokumentbeskrivelse
    FOR EACH ROW EXECUTE FUNCTION process_dokbeskrivelse();

DROP TRIGGER IF EXISTS delete_orphan_dokbesk_ms ON møtesaksregistrering_dokumentbeskrivelse;
CREATE TRIGGER delete_orphan_dokbesk_ms
    AFTER DELETE ON møtesaksregistrering_dokumentbeskrivelse
    FOR EACH ROW EXECUTE FUNCTION process_dokbeskrivelse();

DROP TRIGGER IF EXISTS delete_orphan_dokbesk_md ON møtedokumentregistrering_dokumentbeskrivelse;
CREATE TRIGGER delete_orphan_dokbesk_md
    AFTER DELETE ON møtedokumentregistrering_dokumentbeskrivelse
    FOR EACH ROW EXECUTE FUNCTION process_dokbeskrivelse();
