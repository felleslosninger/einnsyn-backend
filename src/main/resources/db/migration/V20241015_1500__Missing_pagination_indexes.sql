/**
 * Indexes for heavy paginateAsc, paginateDesc repository queries
 */
CREATE INDEX IF NOT EXISTS arkiv__id_parent_journalenhet_idx ON arkiv (_id, parentarkiv_id, journalenhet__id);

CREATE INDEX IF NOT EXISTS arkivdel__id_arkiv_idx ON arkivdel (_id, arkiv_id);

CREATE INDEX IF NOT EXISTS klasse__id_arkivdel_klasse_idx ON klasse (_id, arkivdel_id, klasse_id);

CREATE INDEX IF NOT EXISTS lagretsak__id_saksmappe_moetemappe_idx ON lagret_sak (_id, saksmappe__id, moetemappe__id);

CREATE INDEX IF NOT EXISTS moetemappe__id_arkiv_arkivdel_klasse_utvalg_idx ON møtemappe (
  _id,
  arkiv_id,
  arkivdel_id,
  klasse_id,
  utvalg__id
);

CREATE INDEX IF NOT EXISTS moetesak__id_admenhet_idx ON møtesaksregistrering (_id, administrativ_enhet__id);

CREATE INDEX IF NOT EXISTS saksmappe__id_arkiv_arkivdel_klasse_admenhet ON saksmappe(
  _id,
  arkiv_id,
  arkivdel_id,
  klasse_id,
  administrativ_enhet__id
);
