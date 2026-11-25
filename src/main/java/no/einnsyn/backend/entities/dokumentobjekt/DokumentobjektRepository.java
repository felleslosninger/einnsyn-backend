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

  // Some legacy rows have no _id. They can't be downloaded through the API, so they are skipped.
  @Query(
      """
      SELECT o.id FROM Dokumentobjekt o
      WHERE o.referanseDokumentfil IS NOT NULL
        AND o.id IS NOT NULL
      ORDER BY o.linkCheckedAt ASC NULLS FIRST
      """)
  List<String> findIdsByOldestLinkCheck(Pageable pageable);

  // The link updates below are bulk updates, so `_updated` is not touched and nothing is reindexed.
  // They only apply when the stored URL is still the one that was checked, so a check of an old URL
  // can't overwrite the status of a new one.

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET
        o.linkCheckedAt = :checkedAt,
        o.linkFailedSince = NULL
      WHERE o.id = :id AND o.referanseDokumentfil = :url
      """)
  void setLinkOk(String id, String url, Instant checkedAt);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET
        o.linkCheckedAt = :checkedAt,
        o.linkFailedSince = COALESCE(o.linkFailedSince, :checkedAt)
      WHERE o.id = :id AND o.referanseDokumentfil = :url
      """)
  void setLinkFailed(String id, String url, Instant checkedAt);

  // Only moves the link to the back of the queue, e.g. on 429 Too Many Requests, which doesn't
  // tell whether the link works. Its status is left as it was.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET o.linkCheckedAt = :checkedAt
      WHERE o.id = :id AND o.referanseDokumentfil = :url
      """)
  void setLinkChecked(String id, String url, Instant checkedAt);

  // Runs in the caller's transaction, so the reset is rolled back if the update fails
  @Modifying
  @Query(
      """
      UPDATE Dokumentobjekt o
      SET
        o.linkCheckedAt = NULL,
        o.linkFailedSince = NULL
      WHERE o.id = :id
      """)
  void resetLinkStatus(String id);
}
