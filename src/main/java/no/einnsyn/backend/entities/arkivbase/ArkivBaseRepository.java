package no.einnsyn.backend.entities.arkivbase;

import java.util.List;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ArkivBaseRepository<T extends ArkivBase> extends BaseRepository<T> {

  T findBySystemId(String systemId);

  List<T> findByJournalenhet(Enhet journalenhet);

  List<T> findByExternalIdInAndJournalenhet(List<String> externalIds, Enhet journalenhet);

  T findByExternalIdAndJournalenhet(String externalId, Enhet journalenhet);
}
