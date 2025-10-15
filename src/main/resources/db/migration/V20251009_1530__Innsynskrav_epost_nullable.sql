ALTER TABLE innsynskrav
    ALTER COLUMN epost DROP NOT NULL;

-- When the old api anonymizes innsynskrav by blanking 'epost' and 'bruker_iri' we must also set 'bruker__id' to null.
CREATE OR REPLACE FUNCTION clearUserWhenIriIsBlank()
    RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.bruker_iri IS null) THEN
        NEW.bruker__id := null;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS clear_user_id_when_iri_is_blank_innsynskrav_trigger ON innsynskrav;
CREATE TRIGGER clear_user_id_when_iri_is_blank_innsynskrav_trigger BEFORE UPDATE ON innsynskrav
    FOR EACH ROW EXECUTE FUNCTION clearUserWhenIriIsBlank();