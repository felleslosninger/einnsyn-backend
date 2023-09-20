package no.einnsyn.apiv3.entities.saksmappe;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

public interface SaksmappeRepository extends CrudRepository<Saksmappe, Long> {

  public Boolean existsById(String id);

  public Saksmappe findById(String id);

  public Saksmappe findByExternalId(String externalId);

  public void deleteById(String id);
}
