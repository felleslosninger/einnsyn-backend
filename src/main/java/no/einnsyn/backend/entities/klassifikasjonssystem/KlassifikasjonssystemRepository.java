package no.einnsyn.backend.entities.klassifikasjonssystem;

import java.util.stream.Stream;
import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.arkivdel.models.Arkivdel;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.Klassifikasjonssystem;
import org.springframework.data.jpa.repository.Query;

public interface KlassifikasjonssystemRepository
    extends ArkivBaseRepository<Klassifikasjonssystem> {
  @Query("SELECT o.id FROM Klassifikasjonssystem o WHERE arkivdel = :arkivdel")
  Stream<String> findIdsByArkivdel(Arkivdel arkivdel);
}
