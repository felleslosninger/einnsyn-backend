CREATE OR REPLACE FUNCTION enrich_legacy_enhet()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW._external_id IS NULL
  AND NEW.iri IS NOT NULL
  AND NEW.iri COLLATE "default" != NEW._id COLLATE "default"
  THEN
    NEW._external_id := NEW.iri;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;