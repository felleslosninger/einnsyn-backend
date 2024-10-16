package no.einnsyn.apiv3.entities.bruker;

import no.einnsyn.apiv3.entities.base.BaseRepository;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;

public interface BrukerRepository extends BaseRepository<Bruker> {

  Bruker findByEmail(String email);
}
