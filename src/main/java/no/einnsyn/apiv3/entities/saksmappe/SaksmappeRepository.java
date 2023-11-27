package no.einnsyn.apiv3.entities.saksmappe;

import java.util.stream.Stream;
import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;

public interface SaksmappeRepository extends EinnsynRepository<Saksmappe, Integer> {

  Stream<Saksmappe> findByJournalenhet(Enhet enhet);
}
