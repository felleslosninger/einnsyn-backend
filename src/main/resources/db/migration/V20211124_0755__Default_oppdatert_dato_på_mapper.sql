alter table saksmappe
    alter column oppdatert_dato set default (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
alter table møtemappe
    alter column oppdatert_dato set default (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
