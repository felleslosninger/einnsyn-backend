package no.einnsyn.apiv3.entities.arkivbase;

import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ArkivBaseRepository<T extends ArkivBase> extends BaseRepository<T> {

  T findBySystemId(String externalId);

  boolean existsBySystemId(String externalId);
}
