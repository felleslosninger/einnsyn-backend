package no.einnsyn.apiv3.entities.enhet;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

public interface EnhetRepository extends CrudRepository<Enhet, UUID> {

  public boolean existsById(String id);

  public Enhet findById(String id);

  public Enhet findByLegacyId(UUID id);
}
