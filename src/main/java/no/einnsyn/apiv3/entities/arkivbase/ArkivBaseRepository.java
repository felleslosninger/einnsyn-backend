package no.einnsyn.apiv3.entities.arkivbase;

import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ArkivBaseRepository<T extends ArkivBase> extends BaseRepository<T> {

  T findBySystemId(String externalId);

  boolean existsBySystemId(String externalId);

  Page<T> findByJournalenhet(Enhet enhet, Pageable pageable);
}
