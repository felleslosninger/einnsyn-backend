package no.einnsyn.apiv3.entities.skjerming;

import org.springframework.data.repository.CrudRepository;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;

public interface SkjermingRepository extends CrudRepository<Skjerming, Long> {

  public Skjerming findById(String id);
  
}
