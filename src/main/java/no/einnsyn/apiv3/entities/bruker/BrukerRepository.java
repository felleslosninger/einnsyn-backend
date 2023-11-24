package no.einnsyn.apiv3.entities.bruker;

import java.util.UUID;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;


public interface BrukerRepository extends EinnsynRepository<Bruker, UUID> {

  public boolean existsByEmail(String email);

  public Bruker findByEmail(String email);

}
