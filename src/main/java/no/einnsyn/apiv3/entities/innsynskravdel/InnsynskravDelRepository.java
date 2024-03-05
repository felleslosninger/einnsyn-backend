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
import org.springframework.data.jpa.repository.Query;

public interface InnsynskravDelRepository extends BaseRepository<InnsynskravDel> {

  Stream<InnsynskravDel> findAllByEnhet(Enhet enhet);

  Stream<InnsynskravDel> findAllByJournalpost(Journalpost journalpost);

  @Query(
      "SELECT o FROM InnsynskravDel o WHERE o.innsynskrav = :innsynskrav AND (:pivot IS NULL OR"
          + " o.id >= :pivot) ORDER BY o.id ASC")
  Page<InnsynskravDel> paginateAsc(Innsynskrav innsynskrav, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM InnsynskravDel o WHERE o.innsynskrav = :innsynskrav AND (:pivot IS NULL OR"
          + " o.id <= :pivot) ORDER BY o.id DESC")
  Page<InnsynskravDel> paginateDesc(Innsynskrav innsynskrav, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM InnsynskravDel o JOIN o.innsynskrav i WHERE i.bruker = :bruker AND (:pivot IS"
          + " NULL OR o.id >= :pivot) ORDER BY o.id ASC")
  Page<InnsynskravDel> paginateAsc(Bruker bruker, String pivot, Pageable pageable);

  @Query(
      "SELECT o FROM InnsynskravDel o JOIN o.innsynskrav i WHERE i.bruker = :bruker AND (:pivot IS"
          + " NULL OR o.id <= :pivot) ORDER BY o.id DESC")
  Page<InnsynskravDel> paginateDesc(Bruker bruker, String pivot, Pageable pageable);
}
