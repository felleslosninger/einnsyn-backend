-- TODO: These triggers should be removed when the legacy import is killed

-- journalpost
CREATE OR REPLACE FUNCTION sync_external_id_journalpost()
    RETURNS TRIGGER AS $$
BEGIN
    -- Always set journalpost_iri to _external_id
    IF (NEW.journalpost_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.journalpost_iri := NEW._external_id;
        -- Update referencing møtesak if journalposttype is saksframlegg
        IF (NEW.journalposttype = 'http://www.arkivverket.no/standarder/noark5/arkivstruktur/saksframlegg') THEN
            update møtesaksregistrering set journalpost_iri = NEW._external_id where journalpost_iri = OLD._external_id;
        end if;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_journalpost_trigger ON journalpost;
CREATE TRIGGER sync_external_id_journalpost_trigger BEFORE UPDATE ON journalpost
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_journalpost();

--
CREATE OR REPLACE FUNCTION enrich_legacy_journalpost()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set _external_id to journalpost_iri for old import
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.journalpost_iri IS NOT NULL AND NEW.journalpost_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.journalpost_iri IS DISTINCT FROM OLD.journalpost_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.journalpost_iri;
    END IF;
    -- Set saksmappe_iri for new import
    IF NEW.saksmappe_iri IS NULL AND NEW.saksmappe_id IS NOT NULL THEN
        SELECT _external_id INTO NEW.saksmappe_iri FROM saksmappe WHERE saksmappe_id = NEW.saksmappe_id;
    END IF;
    -- Set administrativ_enhet__id from arkivskaper for old import
    IF (NEW.administrativ_enhet__id IS NULL AND NEW.arkivskaper IS NOT NULL)
        OR (TG_OP = 'UPDATE'
            AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper
            AND NEW.administrativ_enhet__id IS NOT DISTINCT FROM OLD.administrativ_enhet__id
           ) THEN
        SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_journalpost_trigger ON journalpost;
CREATE TRIGGER enrich_legacy_journalpost_trigger BEFORE INSERT OR UPDATE ON journalpost
    FOR EACH ROW EXECUTE FUNCTION enrich_legacy_journalpost();
