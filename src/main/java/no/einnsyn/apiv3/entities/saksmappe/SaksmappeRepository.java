package no.einnsyn.apiv3.entities.saksmappe;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

public interface SaksmappeRepository extends CrudRepository<Saksmappe, Long> {

  public Optional<Saksmappe> findById(String id);

  public Optional<Saksmappe> findByExternalId(String externalId);

  public void deleteByExternalId(String externalId);
}
