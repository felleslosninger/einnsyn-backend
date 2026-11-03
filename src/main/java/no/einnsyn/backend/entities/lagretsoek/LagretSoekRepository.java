package no.einnsyn.backend.entities.lagretsoek;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoek;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LagretSoekRepository
    extends BaseRepository<LagretSoek>, IndexableRepository<LagretSoek> {

  LagretSoek findByLegacyId(UUID legacyId);

  @Query(
      """
      SELECT e FROM LagretSoek e WHERE
      subscribe = true AND
      hitCount > 0 AND
      bruker.id = :brukerId
      """)
  List<LagretSoek> findLagretSoekWithHitsByBruker(String brukerId);

  @Query(
      """
      SELECT DISTINCT bruker.id FROM LagretSoek WHERE
      subscribe = true AND
      hitCount > 0
      """)
  Stream<String> streamBrukerIdWithLagretSoekHits();

  /** Rows are locked in ID order, as in {@link #addHitByIds}, which may update the same rows. */
  @Modifying
  @Query(
      value =
          """
          WITH locked AS (
            SELECT _id FROM lagret_sok
            WHERE _id IN (:idList)
            ORDER BY _id
            FOR NO KEY UPDATE
          )
          UPDATE lagret_sok
          SET hit_count = 0
          FROM locked
          WHERE lagret_sok._id = locked._id
          """,
      nativeQuery = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void resetHitCount(List<String> idList);

  @Modifying
  @Query("DELETE FROM LagretSoekHit WHERE lagretSoek.id IN :idList")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void deleteHits(List<String> idList);

  @Query("SELECT o.id FROM LagretSoek o WHERE bruker.id = :brukerId ORDER BY id DESC")
  Stream<String> streamIdByBrukerId(String brukerId);

  /**
   * Count one hit on each of the given LagretSoek. Rows are locked in ID order, as in {@link
   * #resetHitCount}, since concurrent matchers and notifications update overlapping sets of rows
   * and would otherwise deadlock.
   *
   * @return the IDs of those whose hit should be cached for the notification
   */
  @Query(
      value =
          """
          WITH locked AS (
            SELECT _id FROM lagret_sok
            WHERE _id IN (:ids)
            AND abonnere = true
            AND hit_count < 101
            ORDER BY _id
            FOR NO KEY UPDATE
          ), updated AS (
            UPDATE lagret_sok
            SET hit_count = lagret_sok.hit_count + 1
            FROM locked
            WHERE lagret_sok._id = locked._id
            RETURNING lagret_sok._id, lagret_sok.hit_count
          )
          SELECT _id FROM updated WHERE hit_count <= 10
          """,
      nativeQuery = true)
  @Transactional
  List<String> addHitByIds(List<String> ids);

  @Query(
      """
      SELECT o FROM LagretSoek o
      WHERE bruker = :bruker
      AND (:pivot IS NULL OR id >= :pivot)
      ORDER BY id ASC
      """)
  Slice<LagretSoek> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM LagretSoek o
      WHERE bruker = :bruker
      AND (:pivot IS NULL OR id <= :pivot)
      ORDER BY id DESC
      """)
  Slice<LagretSoek> paginateDesc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      value =
          """
          SELECT _id FROM lagret_sok WHERE last_indexed IS NULL
          UNION ALL
          SELECT _id FROM lagret_sok WHERE last_indexed < _updated
          UNION ALL
          SELECT _id FROM lagret_sok WHERE last_indexed < :schemaVersion
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  Stream<String> streamUnIndexed(Instant schemaVersion);

  @Query(
      value =
          """
          WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id)
          SELECT ids._id
          FROM ids
          LEFT JOIN lagret_sok AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  List<String> findNonExistingIds(String[] ids);

  @Query(
      """
      SELECT o.id FROM LagretSoek o
      WHERE o.legacyQueryEs IS NOT NULL
      AND o.searchParameters IS NULL
      """)
  Stream<String> streamLegacyLagretSoek();
}
