package no.einnsyn.backend.entities.klassifikasjonssystem;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  Stream<Klassifikasjonssystem> findByArkivdel(Arkivdel arkivdel);
}
