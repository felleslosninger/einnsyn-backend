package no.einnsyn.apiv3.entities.utredning;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.utredning.models.Utredning;
import org.springframework.data.jpa.repository.Query;

public interface UtredningRepository extends ArkivBaseRepository<Utredning> {

  @Query(
      "SELECT COUNT(u) FROM Utredning u JOIN u.utredningsdokument d WHERE d ="
          + " :utredningsdokument")
  int countByUtredningsdokument(Dokumentbeskrivelse utredningsdokument);
}
