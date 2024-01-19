CREATE UNIQUE INDEX IF NOT EXISTS rettet_mot_sok_id_meldt
  ON lagret_sok_treff (lagret_sok_id, rettet_mot, COALESCE(meldt_til_bruker_tid, '1970-01-01 00:00:00.000+00'));

CREATE UNIQUE INDEX IF NOT EXISTS rettet_mot_sak_id_meldt
  ON lagret_sak_treff (lagret_sak_id, rettet_mot, COALESCE(meldt_til_bruker_tid, '1970-01-01 00:00:00.000+00'));
