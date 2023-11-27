package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;


public interface InnsynskravDelRepository extends EinnsynRepository<InnsynskravDel, UUID> {

  public Page<InnsynskravDel> findByEnhet(Enhet enhet, Pageable pageable);

  public List<InnsynskravDel> findByEnhet(Enhet enhet);

  public List<InnsynskravDel> findByJournalpost(Journalpost journalpost);

  public void deleteByJournalpost(Journalpost journalpost);
}
