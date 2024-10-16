package no.einnsyn.apiv3.entities.arkivbase;

import java.util.List;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ArkivBaseRepository<T extends ArkivBase> extends BaseRepository<T> {

  T findBySystemId(String systemId);

  List<T> findByJournalenhet(Enhet journalenhet);

  List<T> findByExternalIdInAndJournalenhet(List<String> externalIds, Enhet journalenhet);

  T findByExternalIdAndJournalenhet(String externalId, Enhet journalenhet);
}
