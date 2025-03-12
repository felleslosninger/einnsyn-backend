CREATE OR REPLACE FUNCTION set_accessible_after_on_insert()
    RETURNS TRIGGER AS
$body$
BEGIN
    IF
        NEW._accessible_after is null
    THEN
        NEW._accessible_after = now();
    END IF;
    RETURN NEW;
END;
$body$
    LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_accessible_after_enhet ON enhet;
CREATE TRIGGER tr_accessible_after_enhet
    BEFORE INSERT ON enhet
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_saksmappe ON saksmappe;
CREATE TRIGGER tr_accessible_after_saksmappe
    BEFORE INSERT ON saksmappe
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_journalpost ON journalpost;
CREATE TRIGGER tr_accessible_after_journalpost
    BEFORE INSERT ON journalpost
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_moetemappe ON møtemappe;
CREATE TRIGGER tr_accessible_after_moetemappe
    BEFORE INSERT ON møtemappe
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_moetesak ON møtesaksregistrering;
CREATE TRIGGER tr_accessible_after_moetesak
    BEFORE INSERT ON møtesaksregistrering
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_moetedokument ON møtedokumentregistrering;
CREATE TRIGGER tr_accessible_after_moetedokument
    BEFORE INSERT ON møtedokumentregistrering
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_api_key ON api_key;
CREATE TRIGGER tr_accessible_after_api_key
    BEFORE INSERT ON api_key
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_arkiv ON arkiv;
CREATE TRIGGER tr_accessible_after_arkiv
    BEFORE INSERT ON arkiv
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_arkivdel ON arkivdel;
CREATE TRIGGER tr_accessible_after_arkivdel
    BEFORE INSERT ON arkivdel
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_beh_protokoll ON behandlingsprotokoll;
CREATE TRIGGER tr_accessible_after_beh_protokoll
    BEFORE INSERT ON behandlingsprotokoll
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_bruker ON bruker;
CREATE TRIGGER tr_accessible_after_bruker
    BEFORE INSERT ON bruker
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_dok_beskrivelse ON dokumentbeskrivelse;
CREATE TRIGGER tr_accessible_after_dok_beskrivelse
    BEFORE INSERT ON dokumentbeskrivelse
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_dok_objekt ON dokumentobjekt;
CREATE TRIGGER tr_accessible_after_dok_objekt
    BEFORE INSERT ON dokumentobjekt
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_identifikator ON identifikator;
CREATE TRIGGER tr_accessible_after_identifikator
    BEFORE INSERT ON identifikator
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_innsynskrav ON innsynskrav;
CREATE TRIGGER tr_accessible_after_innsynskrav
    BEFORE INSERT ON innsynskrav
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_innsynskrav_del ON innsynskrav_del;
CREATE TRIGGER tr_accessible_after_innsynskrav_del
    BEFORE INSERT ON innsynskrav_del
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_klasse ON klasse;
CREATE TRIGGER tr_accessible_after_klasse
    BEFORE INSERT ON klasse
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_klassifikasjonssystem ON klassifikasjonssystem;
CREATE TRIGGER tr_accessible_after_klassifikasjonssystem
    BEFORE INSERT ON klassifikasjonssystem
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_korr_part ON korrespondansepart;
CREATE TRIGGER tr_accessible_after_korr_part
    BEFORE INSERT ON korrespondansepart
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_lagret_sak ON lagret_sak;
CREATE TRIGGER tr_accessible_after_lagret_sak
    BEFORE INSERT ON lagret_sak
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_lagret_sok ON lagret_sok;
CREATE TRIGGER tr_accessible_after_lagret_sok
    BEFORE INSERT ON lagret_sok
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_lagret_sok_treff ON lagret_sok_treff;
CREATE TRIGGER tr_accessible_after_lagret_sok_treff
    BEFORE INSERT ON lagret_sok_treff
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_lagret_soek_hit ON lagret_soek_hit;
CREATE TRIGGER tr_accessible_after_lagret_soek_hit
    BEFORE INSERT ON lagret_soek_hit
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_skjerming ON skjerming;
CREATE TRIGGER tr_accessible_after_skjerming
    BEFORE INSERT ON skjerming
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_tilbakemelding ON tilbakemelding;
CREATE TRIGGER tr_accessible_after_tilbakemelding
    BEFORE INSERT ON tilbakemelding
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_utredning ON utredning;
CREATE TRIGGER tr_accessible_after_utredning
    BEFORE INSERT ON utredning
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_vedtak ON vedtak;
CREATE TRIGGER tr_accessible_after_vedtak
    BEFORE INSERT ON vedtak
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_votering ON votering;
CREATE TRIGGER tr_accessible_after_votering
    BEFORE INSERT ON votering
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_moetesaksbeskrivelse ON moetesaksbeskrivelse;
CREATE TRIGGER tr_accessible_after_moetesaksbeskrivelse
    BEFORE INSERT ON moetesaksbeskrivelse
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();

DROP TRIGGER IF EXISTS tr_accessible_after_moetedeltaker ON moetedeltaker;
CREATE TRIGGER tr_accessible_after_moetedeltaker
    BEFORE INSERT ON moetedeltaker
    FOR EACH ROW EXECUTE PROCEDURE set_accessible_after_on_insert();
