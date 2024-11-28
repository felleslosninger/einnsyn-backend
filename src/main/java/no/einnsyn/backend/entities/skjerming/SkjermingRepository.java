package no.einnsyn.backend.entities.skjerming;

import no.einnsyn.backend.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.skjerming.models.Skjerming;

public interface SkjermingRepository extends ArkivBaseRepository<Skjerming> {

  public Skjerming findBySkjermingshjemmelAndTilgangsrestriksjonAndJournalenhet(
      String skjermingshjemmel, String tilgangsrestriksjon, Enhet journalenhet);
}
