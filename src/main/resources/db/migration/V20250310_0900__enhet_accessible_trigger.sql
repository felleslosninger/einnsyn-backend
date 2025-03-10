CREATE OR REPLACE FUNCTION enrich_enhet_accessible_after()
    RETURNS TRIGGER AS
$body$
BEGIN
    IF
        NEW._accessible_after is null
    THEN
        NEW._accessible_after = now();
    END IF;
    RETURN NEW;
END;
$body$
    LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_accessible_after_enhet ON enhet;
CREATE TRIGGER tr_accessible_after_enhet
    BEFORE INSERT ON enhet
    FOR EACH ROW EXECUTE PROCEDURE enrich_enhet_accessible_after();
