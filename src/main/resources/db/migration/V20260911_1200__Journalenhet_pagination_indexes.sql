/**
 * Indexes for the journalenhet-filtered pagination queries on the entities that are not unique by
 * IRI / system_id, and are therefore listed per journalenhet
 * (WHERE journalenhet__id = ? AND _id >= ? ORDER BY _id).
 *
 * The composite indexes also serve every lookup the single-column journalenhet__id indexes served,
 * so those are dropped.
 */
CREATE INDEX IF NOT EXISTS arkiv_journalenhet__id__id_idx ON arkiv (journalenhet__id, _id);
DROP INDEX IF EXISTS arkiv_journalenhet__id;

CREATE INDEX IF NOT EXISTS arkivdel_journalenhet__id__id_idx ON arkivdel (journalenhet__id, _id);
DROP INDEX IF EXISTS arkivdel_journalenhet__id;

CREATE INDEX IF NOT EXISTS klasse_journalenhet__id__id_idx ON klasse (journalenhet__id, _id);
