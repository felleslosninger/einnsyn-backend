package no.einnsyn.backend.entities.registrering;

import no.einnsyn.backend.common.hasslug.HasSlugRepository;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.registrering.models.Registrering;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RegistreringRepository<T extends Registrering>
    extends ArkivBaseRepository<T>, HasSlugRepository<T> {}
