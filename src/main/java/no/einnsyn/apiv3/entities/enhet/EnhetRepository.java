package no.einnsyn.apiv3.entities.enhet;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface EnhetRepository extends BaseRepository<Enhet> {

  Enhet findByOrgnummer(String orgnummer);

  /**
   * Search the subtre under `rootId` for the enhetskode `enhetskode`.
   *
   * @param enhetskode
   * @param rootId
   * @return
   */
  @Query(
      value =
          """
WITH RECURSIVE descendants AS (
  SELECT e1.*, 1 AS depth
  FROM enhet e1
  WHERE e1._id = :rootId
  UNION ALL
  SELECT e2.*, d.depth + 1
  FROM enhet e2
  INNER JOIN descendants d ON e2.parent_id = d.id
  WHERE d.depth < 20
)
SELECT * FROM descendants
WHERE enhets_kode ~ CONCAT('(^\\s*|\\s*;\\s*)',
  regexp_replace(:enhetskode, '([\\^\\$\\.\\+\\|\\?\\*\\(\\)\\{\\}\\[\\]\\\\])', '\\\\\\1', 'g'),
  '(\\s*;\\s*|\\s*$)')
LIMIT 1;
""",
      nativeQuery = true)
  Enhet findByEnhetskode(String enhetskode, String rootId);

  /**
   * Check if `childId` is a descendant of `rootId`.
   *
   * @param rootId
   * @param childId
   * @return
   */
  @Query(
      value =
          """
          WITH RECURSIVE ancestors AS (
            SELECT e1._id, e1.id, e1.parent_id, 1 AS depth
            FROM enhet e1
            WHERE e1._id = :childId
            UNION ALL
            SELECT e2._id, e2.id, e2.parent_id, a.depth + 1
            FROM enhet e2
            INNER JOIN ancestors a ON e2.id = a.parent_id
            WHERE a.depth < 20
          )
          SELECT EXISTS (
            SELECT 1
            FROM ancestors
            WHERE _id = :rootId
          );
          """,
      nativeQuery = true)
  boolean isAncestorOf(String rootId, String childId);

  @Query(
      "SELECT o FROM Enhet o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id >= :pivot)"
          + " ORDER BY o.id ASC")
  Page<Enhet> paginateAsc(Enhet parent, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM Enhet o WHERE o.parent = :parent AND (:pivot IS NULL OR o.id <= :pivot)"
          + " ORDER BY o.id DESC")
  Page<Enhet> paginateDesc(Enhet parent, String pivot, Pageable pageable);
}
