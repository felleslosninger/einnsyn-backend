package no.einnsyn.backend.entities.lagretsoek;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoek;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LagretSoekRepository extends BaseRepository<LagretSoek> {

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
}
