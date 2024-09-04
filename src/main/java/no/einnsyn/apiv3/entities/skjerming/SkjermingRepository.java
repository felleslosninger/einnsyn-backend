package no.einnsyn.apiv3.entities.skjerming;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.skjerming.models.Skjerming;

public interface SkjermingRepository extends ArkivBaseRepository<Skjerming> {

  public Skjerming findBySkjermingshjemmelAndTilgangsrestriksjonAndJournalenhet(
      String skjermingshjemmel, String tilgangsrestriksjon, Enhet journalenhet);
}
