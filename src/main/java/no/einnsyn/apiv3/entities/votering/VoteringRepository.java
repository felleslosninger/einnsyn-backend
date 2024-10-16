package no.einnsyn.apiv3.entities.votering;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.identifikator.models.Identifikator;
import no.einnsyn.apiv3.entities.moetedeltaker.models.Moetedeltaker;
import no.einnsyn.apiv3.entities.votering.models.Votering;

public interface VoteringRepository extends ArkivBaseRepository<Votering> {

  boolean existsByMoetedeltaker(Moetedeltaker moetedeltaker);

  boolean existsByRepresenterer(Identifikator representerer);
}
