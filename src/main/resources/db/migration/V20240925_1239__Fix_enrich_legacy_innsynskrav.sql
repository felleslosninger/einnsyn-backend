CREATE OR REPLACE FUNCTION enrich_legacy_innsynskrav()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.bruker__id IS NULL
  AND NEW.bruker_iri IS NOT NULL
  AND NEW.bruker_iri != 'http://data.einnsyn.no/bruker/anonym'
  THEN
    SELECT bruker._id INTO NEW.bruker__id FROM bruker
    WHERE replace(NEW.bruker_iri, 'http://data.einnsyn.no/bruker/', '')::uuid = bruker.id;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;