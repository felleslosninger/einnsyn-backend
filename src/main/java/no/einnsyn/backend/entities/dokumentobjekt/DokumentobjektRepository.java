package no.einnsyn.backend.entities.dokumentobjekt;

import java.time.Instant;
import java.util.List;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.dokumentobjekt.models.Dokumentobjekt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface DokumentobjektRepository extends ArkivBaseRepository<Dokumentobjekt> {

  @Query(
      """
      SELECT o.id FROM Dokumentobjekt o
      WHERE o.linkFailedSince IS NOT NULL
      AND o.linkCheckedAt < :checkedBefore
      ORDER BY o.linkCheckedAt ASC
      """)
  List<String> findIdsWithFailedLinkCheckedBefore(Instant checkedBefore, Pageable pageable);

  @Query(
      """
      SELECT o.id FROM Dokumentobjekt o
      WHERE o.referanseDokumentfil IS NOT NULL
      ORDER BY o.linkCheckedAt ASC NULLS FIRST
      """)
  List<String> findIdsByOldestLinkCheck(Pageable pageable);

  // The link updates below are bulk updates, so `_updated` is not touched and nothing is reindexed.

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET
        o.linkCheckedAt = :checkedAt,
        o.linkFailedSince = NULL
      WHERE o.id = :id
      """)
  void setLinkOk(String id, Instant checkedAt);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET
        o.linkCheckedAt = :checkedAt,
        o.linkFailedSince = COALESCE(o.linkFailedSince, :checkedAt)
      WHERE o.id = :id
      """)
  void setLinkFailed(String id, Instant checkedAt);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query("UPDATE Dokumentobjekt o SET o.linkCheckedAt = :checkedAt WHERE o.id = :id")
  void setLinkChecked(String id, Instant checkedAt);
}
