package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  Page<Klassifikasjonssystem> findByArkivdel(Arkivdel arkivdel, Pageable pageable);
}
