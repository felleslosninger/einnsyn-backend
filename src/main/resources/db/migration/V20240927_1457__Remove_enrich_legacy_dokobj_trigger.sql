-- Dokumentobjekt._external_id can not be globally unique, drop the trigger (for now)
DROP TRIGGER IF EXISTS enrich_legacy_dokumentobjekt_trigger ON dokumentobjekt;

DROP FUNCTION IF EXISTS enrich_legacy_dokumentobjekt()
