-- Missing indexes for foreign key columns

CREATE INDEX IF NOT EXISTS saksmappe_arkivdel_id_idx ON saksmappe (arkivdel_id);
CREATE INDEX IF NOT EXISTS saksmappe_klasse_id_idx ON saksmappe (klasse_id);
ALTER TABLE saksmappe DROP CONSTRAINT IF EXISTS saksmappe_arkiv_id_fkey;

CREATE INDEX IF NOT EXISTS moetemappe_arkivdel_id_idx ON møtemappe (arkivdel_id);
CREATE INDEX IF NOT EXISTS moetemappe_klasse_id_idx ON møtemappe (klasse_id);
ALTER TABLE møtemappe DROP CONSTRAINT IF EXISTS møtemappe_arkiv_id_fkey;

CREATE INDEX IF NOT EXISTS klasse_parentklasse_idx ON klasse (parentklasse);

CREATE INDEX IF NOT EXISTS journalpost_avhendet_til__id_idx ON journalpost (avhendet_til__id);

CREATE INDEX IF NOT EXISTS moetesak_avhendet_til__id_idx ON møtesaksregistrering (avhendet_til__id);

CREATE INDEX IF NOT EXISTS moetedokument_avhendet_til__id_idx ON møtedokumentregistrering (avhendet_til__id);

CREATE INDEX IF NOT EXISTS lagret_soek_hit_journalpost__id_idx ON lagret_soek_hit (journalpost__id);
CREATE INDEX IF NOT EXISTS lagret_soek_hit_lagret_soek__id_idx ON lagret_soek_hit (lagret_soek__id);
CREATE INDEX IF NOT EXISTS lagret_soek_hit_moetemappe__id_idx ON lagret_soek_hit (moetemappe__id);
CREATE INDEX IF NOT EXISTS lagret_soek_hit_moetesak__id_idx ON lagret_soek_hit (moetesak__id);
CREATE INDEX IF NOT EXISTS lagret_soek_hit_saksmappe__id_idx ON lagret_soek_hit (saksmappe__id);

CREATE INDEX IF NOT EXISTS innsynskrav_last_indexed_updated_partial_idx
ON innsynskrav_del (last_indexed)
WHERE (last_indexed < _updated);
