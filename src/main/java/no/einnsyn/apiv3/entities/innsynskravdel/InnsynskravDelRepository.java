package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.stream.Stream;
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

public interface InnsynskravDelRepository extends BaseRepository<InnsynskravDel> {

  Stream<InnsynskravDel> findAllByEnhet(Enhet enhet);

  Stream<InnsynskravDel> findAllByJournalpost(Journalpost journalpost);

  @Transactional
  @Modifying
  @Query("UPDATE InnsynskravDel ind SET ind.sent = CURRENT_TIMESTAMP WHERE ind.id = :id")
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
}
