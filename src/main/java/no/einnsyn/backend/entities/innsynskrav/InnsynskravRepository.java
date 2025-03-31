package no.einnsyn.backend.entities.innsynskrav;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.einnsyn.backend.common.indexable.IndexableRepository;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestilling;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface InnsynskravRepository
    extends BaseRepository<Innsynskrav>, IndexableRepository<Innsynskrav> {

  Stream<Innsynskrav> findAllByEnhet(Enhet enhet);

  Stream<Innsynskrav> findAllByJournalpost(Journalpost journalpost);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Innsynskrav i
      SET
        sent = CURRENT_TIMESTAMP,
        updated = CURRENT_TIMESTAMP
      WHERE id = :id
      """)
  void setSent(String id);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Modifying
  @Query(
      """
      UPDATE Innsynskrav i
      SET
        retryCount = retryCount + 1,
        retryTimestamp = CURRENT_TIMESTAMP
      WHERE id = :id
      """)
  void updateRetries(String id);

  @Modifying
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Query(
      value =
          """
          INSERT INTO innsynskrav_del_status
            (innsynskrav_del_id, opprettet_dato, status, systemgenerert)
          VALUES
            (:legacyId, CURRENT_TIMESTAMP, :status, :systemgenerert)
          """,
      nativeQuery = true)
  void insertLegacyStatusAtomic(UUID legacyId, String status, boolean systemgenerert);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      WHERE innsynskravBestilling = :innsynskravBestilling
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Slice<Innsynskrav> paginateAsc(
      InnsynskravBestilling innsynskravBestilling, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      WHERE innsynskravBestilling = :innsynskravBestilling
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Slice<Innsynskrav> paginateDesc(
      InnsynskravBestilling innsynskravBestilling, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      JOIN o.innsynskravBestilling ib
      WHERE ib.bruker = :bruker
      AND o.id >= COALESCE(:pivot, o.id)
      ORDER BY o.id ASC
      """)
  Slice<Innsynskrav> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      JOIN o.innsynskravBestilling ib
      WHERE ib.bruker = :bruker
      AND o.id <= COALESCE(:pivot, o.id)
      ORDER BY o.id DESC
      """)
  Slice<Innsynskrav> paginateDesc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      JOIN o.journalpost j
      WHERE j.journalenhet = :enhet
      AND o.id >= COALESCE(:pivot, o.id)
      ORDER BY o.id ASC
      """)
  Slice<Innsynskrav> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM Innsynskrav o
      JOIN o.journalpost j
      WHERE j.journalenhet = :enhet
      AND o.id <= COALESCE(:pivot, o.id)
      ORDER BY o.id DESC
      """)
  Slice<Innsynskrav> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      value =
          """
          SELECT _id FROM innsynskrav_del e WHERE e.last_indexed IS NULL
          UNION ALL
          SELECT _id FROM innsynskrav_del e WHERE e.last_indexed < e._updated
          UNION ALL
          SELECT _id FROM innsynskrav_del e WHERE e.last_indexed < :schemaVersion
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  Stream<String> findUnIndexed(Instant schemaVersion);

  @Query(
      value =
          """
          WITH ids AS (SELECT unnest(cast(:ids AS text[])) AS _id)
          SELECT ids._id
          FROM ids
          LEFT JOIN innsynskrav_del AS t ON t._id = ids._id
          WHERE t._id IS NULL
          """,
      nativeQuery = true)
  @Transactional(readOnly = true)
  @Override
  List<String> findNonExistingIds(String[] ids);
}
