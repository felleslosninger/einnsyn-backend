package no.einnsyn.backend.entities.utredning;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.backend.entities.utredning.models.Utredning;
import org.springframework.data.jpa.repository.Query;

public interface UtredningRepository extends ArkivBaseRepository<Utredning> {

  @Query(
      """
      SELECT COUNT(u) FROM Utredning u
      JOIN u.utredningsdokument d
      WHERE d = :utredningsdokument
      """)
  int countByUtredningsdokument(Dokumentbeskrivelse utredningsdokument);
}
