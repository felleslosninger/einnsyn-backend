package no.einnsyn.apiv3.entities.registrering;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.registrering.models.Registrering;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RegistreringRepository<T extends Registrering> extends ArkivBaseRepository<T> {}
