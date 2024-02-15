--lagret_sok_treff insert trigger
CREATE OR REPLACE FUNCTION check_daily_hits_on_lagret_sok_id()
RETURNS TRIGGER AS
$body$
BEGIN
    -- allows 101 (which is limit + 1) un-notified hits per saved search
    IF (SELECT count(*)
		FROM lagret_sok_treff
			where lagret_sok_id = NEW.lagret_sok_id
			and meldt_til_bruker_tid is null) > 101
    THEN
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


-- lagret_sak_treff insert trigger
CREATE OR REPLACE FUNCTION check_daily_hits_on_lagret_sak_id()
RETURNS TRIGGER AS
$body$
BEGIN
    -- allows 101 (which is limit + 1) un-notified hits per saved case
    IF (SELECT count(*)
		FROM lagret_sak_treff
			where lagret_sak_id = NEW.lagret_sak_id
			and meldt_til_bruker_tid is null) > 101
    THEN
		RETURN NULL;
 	END IF;
	RETURN NEW;
END;
$body$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_check_count_unnotified_sak_id ON lagret_sak_treff;
CREATE TRIGGER tr_check_count_unnotified_sak_id
BEFORE INSERT ON lagret_sak_treff
FOR EACH ROW EXECUTE PROCEDURE check_daily_hits_on_lagret_sak_id();
