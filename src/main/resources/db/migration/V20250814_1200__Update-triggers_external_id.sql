-- TODO: These triggers should be removed when the legacy import is killed
-- saksmappe
CREATE OR REPLACE FUNCTION enrich_legacy_saksmappe()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set _external_id to saksmappe_iri for old import
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.saksmappe_iri IS NOT NULL AND NEW.saksmappe_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.saksmappe_iri IS DISTINCT FROM OLD.saksmappe_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.saksmappe_iri;
    END IF;
    -- Set saksmappe_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.saksmappe_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.saksmappe_iri := NEW._external_id;
    END IF;
    -- Look up administrativ_enhet__id for old import
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
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_trigger ON saksmappe;
CREATE TRIGGER enrich_legacy_saksmappe_trigger BEFORE INSERT OR UPDATE ON saksmappe
    FOR EACH ROW EXECUTE FUNCTION enrich_legacy_saksmappe();

-- journalpost
CREATE OR REPLACE FUNCTION enrich_legacy_journalpost()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set _external_id to journalpost_iri for old import
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.journalpost_iri IS NOT NULL AND NEW.journalpost_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.journalpost_iri IS DISTINCT FROM OLD.journalpost_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.journalpost_iri;
    END IF;
    -- Set journalpost_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.journalpost_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.journalpost_iri := NEW._external_id;
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

-- møtemappe
CREATE OR REPLACE FUNCTION enrich_legacy_moetemappe()
    RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.møtemappe_iri IS NOT NULL AND NEW.møtemappe_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.møtemappe_iri IS DISTINCT FROM OLD.møtemappe_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.møtemappe_iri;
    END IF;
    -- Set møtemappe_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtemappe_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtemappe_iri := NEW._external_id;
    END IF;
    -- Look up utvalg__id from "utvalg" if it starts with http:// (it is an iri)
    IF (NEW.utvalg__id IS NULL AND NEW.utvalg LIKE 'http://%')
    OR (TG_OP = 'UPDATE'
        AND NEW.utvalg IS DISTINCT FROM OLD.utvalg
        AND NEW.utvalg LIKE 'http%'
        AND NEW.utvalg__id IS NOT DISTINCT FROM OLD.utvalg__id
    ) THEN
        SELECT _id INTO NEW.utvalg__id FROM enhet WHERE iri = NEW.utvalg;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_moetemappe_trigger ON møtemappe;
CREATE TRIGGER enrich_legacy_moetemappe_trigger BEFORE INSERT OR UPDATE ON møtemappe
    FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetemappe();

-- møtesaksregistrering
CREATE OR REPLACE FUNCTION enrich_legacy_moetesaksregistrering()
    RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.møtesaksregistrering_iri IS NOT NULL AND NEW.møtesaksregistrering_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.møtesaksregistrering_iri IS DISTINCT FROM OLD.møtesaksregistrering_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.møtesaksregistrering_iri;
    END IF;
    -- Set møtesaksregistrering_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtesaksregistrering_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtesaksregistrering_iri := NEW._external_id;
    END IF;
    -- Look up administrativ_enhet__id for old import
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
DROP TRIGGER IF EXISTS enrich_legacy_moetesaksregistrering_trigger ON møtesaksregistrering;
CREATE TRIGGER enrich_legacy_moetesaksregistrering_trigger BEFORE INSERT OR UPDATE ON møtesaksregistrering
    FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetesaksregistrering();

-- møtedokumentregistrering
CREATE OR REPLACE FUNCTION enrich_legacy_moetedokumentregistrering()
    RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW._external_id IS NULL AND NEW.møtedokumentregistrering_iri IS NOT NULL AND NEW.møtedokumentregistrering_iri IS DISTINCT FROM NEW._id)
        OR (TG_OP = 'UPDATE' AND NEW.møtedokumentregistrering_iri IS DISTINCT FROM OLD.møtedokumentregistrering_iri AND NEW._external_id = OLD._external_id) THEN
        NEW._external_id := NEW.møtedokumentregistrering_iri;
    END IF;
    -- Set møtedokumentregistrering_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtedokumentregistrering_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtedokumentregistrering_iri := NEW._external_id;
    END IF;
    -- Look up administrativ_enhet__id for old import
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
DROP TRIGGER IF EXISTS enrich_legacy_moetedokumentregistrering_trigger ON møtedokumentregistrering;
CREATE TRIGGER enrich_legacy_moetedokumentregistrering_trigger BEFORE INSERT OR UPDATE ON møtedokumentregistrering
    FOR EACH ROW EXECUTE FUNCTION enrich_legacy_moetedokumentregistrering();
