/**
 * Indexes for the nested list endpoints that now filter on their parent
 * (WHERE <parent>__id = ? AND _id >= ? ORDER BY _id):
 *   /arkivdel/{id}/klassifikasjonssystem
 *   /klassifikasjonssystem/{id}/klasse
 *   /vedtak/{id}/votering
 *
 * The composite indexes also cover the lookups the single-column parent indexes served, so those
 * are dropped.
 */
CREATE INDEX IF NOT EXISTS klassifikasjonssystem_arkivdel__id__id_idx
  ON klassifikasjonssystem (arkivdel__id, _id);
DROP INDEX IF EXISTS klassifikasjonssystem_arkivdel;

CREATE INDEX IF NOT EXISTS klasse_klassifikasjonssystem__id__id_idx
  ON klasse (klassifikasjonssystem__id, _id);
DROP INDEX IF EXISTS klasse_klassifikasjonssystem__id;

CREATE INDEX IF NOT EXISTS votering_vedtak__id__id_idx ON votering (vedtak__id, _id);
DROP INDEX IF EXISTS votering_vedtak__id;
