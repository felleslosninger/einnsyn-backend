package no.einnsyn.apiv3.entities.bruker;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;

public interface BrukerRepository extends BaseRepository<Bruker> {

  public boolean existsByEmail(String email);

  public Bruker findByEmail(String email);
}
