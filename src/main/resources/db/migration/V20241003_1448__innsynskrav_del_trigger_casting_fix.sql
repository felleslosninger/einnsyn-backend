CREATE OR REPLACE FUNCTION enrich_legacy_innsynskrav_del()
RETURNS TRIGGER AS $$
BEGIN
  -- Update enhet__id for innsynskrav_del from old API
  IF NEW.enhet__id IS NULL AND NEW.virksomhet IS NOT NULL THEN
    SELECT enhet._id INTO NEW.enhet__id FROM enhet WHERE enhet.iri = NEW.virksomhet;
  END IF;
  -- Update journalpost__id for innsynskrav_del from old API
  IF NEW.journalpost__id IS NULL AND NEW.rettet_mot IS NOT NULL THEN
    -- "rettet_mot" is varchar, so we need to cast it to text
    SELECT journalpost._id INTO NEW.journalpost__id
    FROM journalpost WHERE journalpost.journalpost_iri = NEW.rettet_mot::text COLLATE pg_catalog.default;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;