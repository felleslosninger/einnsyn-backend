package no.einnsyn.apiv3.entities.innsynskravdel;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.einnsyn.apiv3.common.indexable.IndexableRepository;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface InnsynskravDelRepository
    extends BaseRepository<InnsynskravDel>, IndexableRepository<InnsynskravDel> {

  Stream<InnsynskravDel> findAllByEnhet(Enhet enhet);

  Stream<InnsynskravDel> findAllByJournalpost(Journalpost journalpost);

  @Transactional
  @Modifying
  @Query(
      """
      UPDATE InnsynskravDel ind
      SET
        sent = CURRENT_TIMESTAMP,
        updated = CURRENT_TIMESTAMP
      WHERE id = :id
      """)
  void setSent(String id);

  @Transactional
  @Modifying
  @Query(
      """
      UPDATE InnsynskravDel ind
      SET
        retryCount = retryCount + 1,
        retryTimestamp = CURRENT_TIMESTAMP
      WHERE id = :id
      """)
  void updateRetries(String id);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      WHERE innsynskrav = :innsynskrav
      AND id >= COALESCE(:pivot, id)
      ORDER BY id ASC
      """)
  Page<InnsynskravDel> paginateAsc(Innsynskrav innsynskrav, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      WHERE innsynskrav = :innsynskrav
      AND id <= COALESCE(:pivot, id)
      ORDER BY id DESC
      """)
  Page<InnsynskravDel> paginateDesc(Innsynskrav innsynskrav, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      JOIN o.innsynskrav i
      WHERE i.bruker = :bruker
      AND o.id >= COALESCE(:pivot, o.id)
      ORDER BY o.id ASC
      """)
  Page<InnsynskravDel> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      JOIN o.innsynskrav i
      WHERE i.bruker = :bruker
      AND o.id <= COALESCE(:pivot, o.id)
      ORDER BY o.id DESC
      """)
  Page<InnsynskravDel> paginateDesc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      JOIN o.journalpost j
      WHERE j.journalenhet = :enhet
      AND o.id >= COALESCE(:pivot, o.id)
      ORDER BY o.id ASC
      """)
  Page<InnsynskravDel> paginateAsc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      """
      SELECT o FROM InnsynskravDel o
      JOIN o.journalpost j
      WHERE j.journalenhet = :enhet
      AND o.id <= COALESCE(:pivot, o.id)
      ORDER BY o.id DESC
      """)
  Page<InnsynskravDel> paginateDesc(Enhet enhet, String pivot, Pageable pageable);

  @Query(
      value =
          """
          SELECT * FROM innsynskrav_del e WHERE e.last_indexed IS NULL
          UNION ALL
          SELECT * FROM innsynskrav_del e WHERE e.last_indexed < e._updated
          UNION ALL
          SELECT * FROM innsynskrav_del e WHERE e.last_indexed < :schemaVersion
          """,
      nativeQuery = true)
  @Override
  Stream<InnsynskravDel> findUnIndexed(Instant schemaVersion);

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
