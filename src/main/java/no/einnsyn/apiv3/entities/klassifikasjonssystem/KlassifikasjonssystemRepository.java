package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.arkivdel.models.Arkivdel;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.Klassifikasjonssystem;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  Stream<Klassifikasjonssystem> findByArkivdel(Arkivdel arkivdel);
}
