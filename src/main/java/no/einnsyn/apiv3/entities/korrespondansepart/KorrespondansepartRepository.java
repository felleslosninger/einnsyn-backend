package no.einnsyn.apiv3.entities.korrespondansepart;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;

public interface KorrespondansepartRepository extends CrudRepository<Korrespondansepart, Long> {

  public Korrespondansepart findById(String id);

}
