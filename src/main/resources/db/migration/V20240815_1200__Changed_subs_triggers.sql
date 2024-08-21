ALTER TABLE lagret_sok
    ADD COLUMN IF NOT EXISTS hit_count int default 0;

--lagret_sok_treff insert trigger
CREATE OR REPLACE FUNCTION check_daily_hits_on_lagret_sok_id()
    RETURNS TRIGGER AS
$body$
BEGIN
    -- allows 101 (which is limit + 1) un-notified hits per saved search

    UPDATE lagret_sok
    SET hit_count = hit_count + 1
    WHERE id = NEW.lagret_sok_id
      AND hit_count <= 100;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    RETURN NEW;

END;
$body$
    LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_check_count_unnotified_sok_id ON lagret_sok_treff;
CREATE TRIGGER tr_check_count_unnotified_sok_id
    BEFORE INSERT ON lagret_sok_treff
    FOR EACH ROW EXECUTE PROCEDURE check_daily_hits_on_lagret_sok_id();
