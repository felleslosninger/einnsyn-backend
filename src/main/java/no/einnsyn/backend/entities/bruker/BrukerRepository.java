package no.einnsyn.backend.entities.bruker;

import no.einnsyn.backend.entities.base.BaseRepository;
import no.einnsyn.backend.entities.bruker.models.Bruker;

public interface BrukerRepository extends BaseRepository<Bruker> {

  Bruker findByEmail(String email);
}
