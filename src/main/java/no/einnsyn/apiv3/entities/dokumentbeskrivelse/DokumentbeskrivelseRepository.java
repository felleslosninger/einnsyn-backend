package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;

public interface DokumentbeskrivelseRepository extends CrudRepository<Korrespondansepart, Long> {
  public Dokumentbeskrivelse findById(String id);
}
