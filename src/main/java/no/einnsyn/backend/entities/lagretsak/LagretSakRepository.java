package no.einnsyn.backend.entities.lagretsak;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.lagretsak.models.LagretSak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LagretSakRepository extends BaseRepository<LagretSak> {

  @Query("SELECT id FROM LagretSak WHERE subscribe = true AND hitCount > 0")
  Stream<String> findLagretSakWithHits();

  @Modifying
  @Query(
      """
      UPDATE LagretSak
      SET hitCount = hitCount + 1
      WHERE saksmappe.id = :mappeId
      AND subscribe = true
      """)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void addHitBySaksmappe(String mappeId);

  @Modifying
  @Query(
      """
      UPDATE LagretSak SET
      hitCount = hitCount + 1
      WHERE moetemappe.id = :mappeId
      AND subscribe = true
      """)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void addHitByMoetemappe(String mappeId);

  @Modifying
  @Query("UPDATE LagretSak SET hitCount = 0 WHERE id = :lagretSakId")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void resetHits(String lagretSakId);

  @Query(
      """
      SELECT o FROM LagretSak o WHERE bruker.id = :brukerId ORDER BY id DESC
      """)
  Stream<LagretSak> findByBruker(String brukerId);

  @Query(
      """
      SELECT o FROM LagretSak o WHERE saksmappe.id = :saksmappeId ORDER BY id DESC
      """)
  Stream<LagretSak> findBySaksmappe(String saksmappeId);

  @Query(
      """
      SELECT o FROM LagretSak o WHERE moetemappe.id = :moetemappeId ORDER BY id DESC
      """)
  Stream<LagretSak> findByMoetemappe(String moetemappeId);

  @Query(
      """
      SELECT o
      FROM LagretSak o
      WHERE bruker.id = :brukerId
      AND saksmappe.id = :saksmappeId
      """)
  LagretSak findByBrukerAndSaksmappe(String brukerId, String saksmappeId);

  @Query(
      """
      SELECT o
      FROM LagretSak o
      WHERE bruker.id = :brukerId
      AND moetemappe.id = :moetemappeId
      """)
  LagretSak findByBrukerAndMoetemappe(String brukerId, String moetemappeId);

  @Query(
      """
      SELECT o
      FROM LagretSak o
      WHERE bruker = :bruker
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<LagretSak> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o
      FROM LagretSak o
      WHERE bruker = :bruker
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<LagretSak> paginateDesc(Bruker bruker, String pivot, Pageable pageable);
}
