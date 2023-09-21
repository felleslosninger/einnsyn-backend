package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;

public interface DokumentbeskrivelseRepository extends CrudRepository<Dokumentbeskrivelse, Long> {
  public Dokumentbeskrivelse findById(String id);
}
