-- Remove legacy sync/enrich triggers. IRI syncing is now handled in Java (@PreUpdate).
-- These triggers were originally created for old import systems that no longer exist.

-- Drop sync_external_id triggers (V20250814, V20250911)
DROP TRIGGER IF EXISTS sync_external_id_saksmappe_trigger ON saksmappe;
DROP FUNCTION IF EXISTS sync_external_id_saksmappe;

DROP TRIGGER IF EXISTS sync_external_id_moetemappe_trigger ON møtemappe;
DROP FUNCTION IF EXISTS sync_external_id_moetemappe;

DROP TRIGGER IF EXISTS sync_external_id_moetesaksregistrering_trigger ON møtesaksregistrering;
DROP FUNCTION IF EXISTS sync_external_id_moetesaksregistrering;

DROP TRIGGER IF EXISTS sync_external_id_moetedokumentregistrering_trigger ON møtedokumentregistrering;
DROP FUNCTION IF EXISTS sync_external_id_moetedokumentregistrering;

-- Replace journalpost sync trigger: only keep the cross-table update
-- (updating møtesaksregistrering.journalpost_iri when a saksframlegg journalpost's IRI changes)
DROP TRIGGER IF EXISTS sync_external_id_journalpost_trigger ON journalpost;
CREATE OR REPLACE FUNCTION sync_external_id_journalpost()
    RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.journalpost_iri IS DISTINCT FROM OLD.journalpost_iri
        AND NEW.journalposttype LIKE '%saksframlegg') THEN
        UPDATE møtesaksregistrering
        SET journalpost_iri = NEW.journalpost_iri
        WHERE journalpost_iri = OLD.journalpost_iri;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER sync_external_id_journalpost_trigger BEFORE UPDATE ON journalpost
    FOR EACH ROW EXECUTE FUNCTION sync_external_id_journalpost();

-- Drop enrich_legacy triggers (V20241007, V20250911)
DROP TRIGGER IF EXISTS enrich_legacy_saksmappe_trigger ON saksmappe;
DROP FUNCTION IF EXISTS enrich_legacy_saksmappe;

DROP TRIGGER IF EXISTS enrich_legacy_journalpost_trigger ON journalpost;
DROP FUNCTION IF EXISTS enrich_legacy_journalpost;

DROP TRIGGER IF EXISTS enrich_legacy_moetemappe_trigger ON møtemappe;
DROP FUNCTION IF EXISTS enrich_legacy_moetemappe;

DROP TRIGGER IF EXISTS enrich_legacy_moetesaksregistrering_trigger ON møtesaksregistrering;
DROP FUNCTION IF EXISTS enrich_legacy_moetesaksregistrering;

DROP TRIGGER IF EXISTS enrich_legacy_moetedokumentregistrering_trigger ON møtedokumentregistrering;
DROP FUNCTION IF EXISTS enrich_legacy_moetedokumentregistrering;

-- Drop enrich_legacy triggers for innsynskrav (V20240911, V20240925, V20241003)
DROP TRIGGER IF EXISTS enrich_legacy_innsynskrav ON innsynskrav;
DROP FUNCTION IF EXISTS enrich_legacy_innsynskrav;

DROP TRIGGER IF EXISTS enrich_legacy_innsynskrav_del ON innsynskrav_del;
DROP FUNCTION IF EXISTS enrich_legacy_innsynskrav_del;
