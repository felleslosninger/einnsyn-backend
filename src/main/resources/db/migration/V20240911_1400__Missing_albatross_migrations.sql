-- Innsynskrav
CREATE OR REPLACE FUNCTION enrich_legacy_innsynskrav()
RETURNS TRIGGER AS $$
BEGIN
  -- Update bruker__id for innsynskrav from old API
  IF NEW.bruker__id IS NULL AND NEW.bruker_iri IS NOT NULL THEN
    SELECT bruker._id INTO NEW.bruker__id FROM bruker WHERE bruker.iri = NEW.bruker_iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS enrich_legacy_innsynskrav ON innsynskrav;
CREATE TRIGGER enrich_legacy_innsynskrav
BEFORE INSERT OR UPDATE ON innsynskrav
FOR EACH ROW EXECUTE FUNCTION enrich_legacy_innsynskrav();


-- InnsynskravDel
CREATE OR REPLACE FUNCTION enrich_legacy_innsynskrav_del()
RETURNS TRIGGER AS $$
BEGIN
  -- Update bruker__id for innsynskrav_del from old API
  IF NEW.enhet__id IS NULL AND NEW.virksomhet IS NOT NULL THEN
    SELECT enhet._id INTO NEW.enhet__id FROM enhet WHERE enhet.iri = NEW.virksomhet;
  END IF;
  -- Update journalpost__id for innsynskrav_del from old API
  IF NEW.journalpost__id IS NULL AND NEW.rettet_mot IS NOT NULL THEN
    SELECT journalpost._id INTO NEW.journalpost__id
    FROM journalpost WHERE journalpost.journalpost_iri = NEW.rettet_mot;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER enrich_legacy_innsynskrav_del
BEFORE INSERT OR UPDATE ON innsynskrav_del
FOR EACH ROW EXECUTE FUNCTION enrich_legacy_innsynskrav_del();


-- Saksmappe
CREATE OR REPLACE FUNCTION enrich_legacy_saksmappe()
RETURNS TRIGGER AS $$
BEGIN
  -- Set _external_id to saksmappe_iri for old import
  IF NEW._external_id IS NULL AND NEW.saksmappe_iri IS NOT NULL AND NEW.saksmappe_iri != NEW._id THEN
    NEW._external_id := NEW.saksmappe_iri;
  END IF;
  -- Update administrativ_enhet__id for old import
  IF (NEW.administrativ_enhet__id IS NULL AND NEW.arkivskaper IS NOT NULL)
  OR (TG_OP = 'UPDATE' AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper) THEN
    SELECT _id INTO NEW.administrativ_enhet__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Insert / Update trigger already exists


-- Moetemappe
CREATE OR REPLACE FUNCTION enrich_legacy_moetemappe()
RETURNS TRIGGER AS $$
BEGIN
  -- Set _external_id to moete_iri for old import
  IF NEW._external_id IS NULL AND NEW.møtemappe_iri IS NOT NULL AND NEW.møtemappe_iri != NEW._id THEN
    NEW._external_id := NEW.møtemappe_iri;
  END IF;
  -- Update utvalg__id for old import
  IF (NEW.utvalg__id IS NULL AND NEW.arkivskaper IS NOT NULL)
  OR (TG_OP = 'UPDATE' AND NEW.arkivskaper IS DISTINCT FROM OLD.arkivskaper) THEN
    SELECT _id INTO NEW.utvalg__id FROM enhet WHERE iri = NEW.arkivskaper;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- Insert / Update trigger already exists
