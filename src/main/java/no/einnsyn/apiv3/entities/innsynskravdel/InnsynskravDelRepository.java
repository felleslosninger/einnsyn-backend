package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface InnsynskravDelRepository extends BaseRepository<InnsynskravDel> {

  Stream<InnsynskravDel> findAllByEnhet(Enhet enhet);

  Stream<InnsynskravDel> findAllByJournalpost(Journalpost journalpost);

  @Query(
      """
        SELECT i
        FROM InnsynskravDel id
        INNER JOIN id.innsynskrav i
        WHERE i.bruker = :bruker
        ORDER BY id.id DESC
      """)
  Page<InnsynskravDel> findByBruker(Bruker bruker, Pageable pageable);

  // @Query("""
  // SELECT i
  // FROM InnsynskravDel id
  // INNER JOIN id.innsynskrav i
  // WHERE i.bruker = :bruker
  // AND id.id > :id
  // ORDER BY id.id DESC
  // """)
  // Page<InnsynskravDel> findByBrukerAndIdGreaterThanOrderBy(Bruker bruker, String id,
  // Pageable pageable);

  // @Query("""
  // SELECT i
  // FROM InnsynskravDel id
  // INNER JOIN id.innsynskrav i
  // WHERE i.bruker = :bruker
  // AND id.id < :id
  // ORDER BY id.id DESC
  // """)
  // Page<InnsynskravDel> findByBrukerAndIdLessThan(Bruker bruker, String id,
  // Pageable pageable);

  void deleteByJournalpost(Journalpost journalpost);
}
