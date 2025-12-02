package no.einnsyn.backend.entities.klassifikasjonssystem;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.jpa.repository.Query;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  @Query("SELECT id FROM Klassifikasjonssystem WHERE arkivdel = :arkivdel")
  Stream<String> streamIdByArkivdel(Arkivdel arkivdel);
}
