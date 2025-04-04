package no.einnsyn.backend.entities.enhet;

import java.util.List;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

public interface EnhetRepository extends BaseRepository<Enhet> {

  Enhet findByOrgnummer(String orgnummer);

  @Query("SELECT e.id FROM Enhet e WHERE skjult = true")
  List<String> findHiddenIds();

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

  /**
   * Find all descendants of `rootId`.
   *
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
          SELECT _id FROM descendants;
          """,
      nativeQuery = true)
  List<String> getSubtreeIdList(String rootId);

  /**
   * Recursively check if `enhet`, or any of its ancestors, is hidden.
   *
   * @param enhet
   * @return
   */
  @Query(
      value =
          """
          WITH RECURSIVE ancestors AS (
            SELECT e1._id, e1.id, e1.parent_id, e1.skjult, 1 AS depth
            FROM enhet e1
            WHERE e1._id = :enhetId
            UNION ALL
            SELECT e2._id, e2.id, e2.parent_id, e2.skjult, a.depth + 1
            FROM enhet e2
            INNER JOIN ancestors a ON e2.id = a.parent_id
            WHERE a.depth < 20
          )
          SELECT EXISTS (
            SELECT 1
            FROM ancestors
            WHERE skjult = true
          );
          """,
      nativeQuery = true)
  boolean isSkjult(String enhetId);

  /**
   * Remove all sub-units from the list of enhetIds.
   *
   * @param enhetIds
   * @return
   */
  @Query(
      value =
          """
          WITH RECURSIVE inputIds AS (
            SELECT unnest(cast(:enhetIds AS text[])) AS _id
          ),
          ancestors AS (
            -- Start with the given enhetIds
            SELECT e1._id, e1.parent_id, 1 AS depth
            FROM enhet e1
            WHERE e1._id IN (SELECT _id FROM inputIds)
            UNION ALL

            -- Recursively find parents
            SELECT e2._id, e2.parent_id, a.depth + 1
            FROM enhet e2
            INNER JOIN ancestors a ON e2.id = a.parent_id
            WHERE e2.parent_id IS NOT NULL AND a.depth < 20
          )

          -- Find ids that doesnt have its parent in the ancestors list
          SELECT inputIds._id FROM inputIds
          WHERE NOT EXISTS(
            SELECT 1
            FROM ancestors a
            JOIN enhet p ON p.id = a.parent_id
            WHERE a._id = inputIds._id
            AND p._id IN (SELECT _id FROM inputIds)
          )
          """,
      nativeQuery = true)
  List<String> stripNestedIds(List<String> enhetIds);

  @Query(
      """
      SELECT o FROM Enhet o
      WHERE parent = :parent
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Enhet> paginateAsc(Enhet parent, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Enhet o
      WHERE parent = :parent
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Enhet> paginateDesc(Enhet parent, String pivot, Pageable pageable);
}
