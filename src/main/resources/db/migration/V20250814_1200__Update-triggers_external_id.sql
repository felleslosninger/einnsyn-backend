-- TODO: These triggers should be removed when the legacy import is killed
-- saksmappe
CREATE OR REPLACE FUNCTION sync_external_id_saksmappe()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set saksmappe_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.saksmappe_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.saksmappe_iri := NEW._external_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_saksmappe_trigger ON saksmappe;
CREATE TRIGGER sync_external_id_saksmappe_trigger BEFORE INSERT OR UPDATE ON saksmappe
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_saksmappe();

-- journalpost
CREATE OR REPLACE FUNCTION sync_external_id_journalpost()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set journalpost_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.journalpost_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.journalpost_iri := NEW._external_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_journalpost_trigger ON journalpost;
CREATE TRIGGER sync_external_id_journalpost_trigger BEFORE INSERT OR UPDATE ON journalpost
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_journalpost();

-- møtemappe
CREATE OR REPLACE FUNCTION sync_external_id_moetemappe()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set møtemappe_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtemappe_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtemappe_iri := NEW._external_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_moetemappe_trigger ON møtemappe;
CREATE TRIGGER sync_external_id_moetemappe_trigger BEFORE INSERT OR UPDATE ON møtemappe
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_moetemappe();

-- møtesaksregistrering
CREATE OR REPLACE FUNCTION sync_external_id_moetesaksregistrering()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set møtesaksregistrering_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtesaksregistrering_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtesaksregistrering_iri := NEW._external_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_moetesaksregistrering_trigger ON møtesaksregistrering;
CREATE TRIGGER sync_external_id_moetesaksregistrering_trigger BEFORE INSERT OR UPDATE ON møtesaksregistrering
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_moetesaksregistrering();

-- møtedokumentregistrering
CREATE OR REPLACE FUNCTION sync_external_id_moetedokumentregistrering()
    RETURNS TRIGGER AS $$
BEGIN
    -- Set møtedokumentregistrering_iri when _external_id changes
    IF (TG_OP = 'UPDATE' AND NEW._external_id IS DISTINCT FROM OLD._external_id AND NEW.møtedokumentregistrering_iri IS DISTINCT FROM NEW._external_id) THEN
        NEW.møtedokumentregistrering_iri := NEW._external_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS sync_external_id_moetedokumentregistrering_trigger ON møtedokumentregistrering;
CREATE TRIGGER sync_external_id_moetedokumentregistrering_trigger BEFORE INSERT OR UPDATE ON møtedokumentregistrering
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_moetedokumentregistrering();
