package no.einnsyn.backend.entities.votering;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.identifikator.models.Identifikator;
import no.einnsyn.backend.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.backend.entities.votering.models.Votering;

public interface VoteringRepository extends ArkivBaseRepository<Votering> {

  boolean existsByMoetedeltaker(Moetedeltaker moetedeltaker);

  boolean existsByRepresenterer(Identifikator representerer);
}
