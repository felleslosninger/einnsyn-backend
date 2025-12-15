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

  @Modifying
  @Query("UPDATE LagretSoek SET hitCount = 0 WHERE id IN :idList")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void resetHitCount(List<String> idList);

  @Modifying
  @Query("DELETE FROM LagretSoekHit WHERE lagretSoek.id IN :idList")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void deleteHits(List<String> idList);

  @Query("SELECT o.id FROM LagretSoek o WHERE bruker.id = :brukerId ORDER BY id DESC")
  Stream<String> streamIdByBrukerId(String brukerId);

  @Query(
      value =
          """
          UPDATE lagret_sok
          SET hit_count = hit_count + 1
          WHERE _id = :id
          AND abonnere = true
          AND hit_count < 101
          RETURNING hit_count
          """,
      nativeQuery = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Integer addHitById(String id);

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
      WHERE o.legacyQuery IS NOT NULL
      AND o.searchParameters IS NULL
      """)
  Stream<String> streamLegacyLagretSoek();
}
