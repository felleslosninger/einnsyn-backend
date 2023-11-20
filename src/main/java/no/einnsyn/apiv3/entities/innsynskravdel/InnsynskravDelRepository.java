package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;


public interface InnsynskravDelRepository extends EinnsynRepository<InnsynskravDel, UUID> {

  public Page<InnsynskravDel> findByEnhet(Enhet enhet, Pageable pageable);

  public List<InnsynskravDel> findByEnhet(Enhet enhet);

  public List<InnsynskravDel> findByJournalpost(Journalpost journalpost);

  @Query("""
        SELECT i
        FROM InnsynskravDel id
        INNER JOIN id.innsynskrav i
        WHERE i.bruker = :bruker
        ORDER BY id.id DESC
      """)
  public Page<InnsynskravDel> findByBruker(Bruker bruker, Pageable pageable);

  // @Query("""
  // SELECT i
  // FROM InnsynskravDel id
  // INNER JOIN id.innsynskrav i
  // WHERE i.bruker = :bruker
  // AND id.id > :id
  // ORDER BY id.id DESC
  // """)
  // public Page<InnsynskravDel> findByBrukerAndIdGreaterThanOrderBy(Bruker bruker, String id,
  // Pageable pageable);

  // @Query("""
  // SELECT i
  // FROM InnsynskravDel id
  // INNER JOIN id.innsynskrav i
  // WHERE i.bruker = :bruker
  // AND id.id < :id
  // ORDER BY id.id DESC
  // """)
  // public Page<InnsynskravDel> findByBrukerAndIdLessThan(Bruker bruker, String id,
  // Pageable pageable);

  public void deleteByJournalpost(Journalpost journalpost);
}
