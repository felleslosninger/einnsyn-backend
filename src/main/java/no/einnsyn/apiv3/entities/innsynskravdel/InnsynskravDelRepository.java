package no.einnsyn.apiv3.entities.innsynskravdel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;

public interface InnsynskravDelRepository extends EinnsynRepository<InnsynskravDel, Long> {

  public Page<InnsynskravDel> findByEnhet(Enhet enhet, Pageable pageable);
}
