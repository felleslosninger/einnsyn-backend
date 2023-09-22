package no.einnsyn.apiv3.entities.enhet;

import java.util.UUID;
import no.einnsyn.apiv3.entities.IEinnsynRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;

public interface EnhetRepository extends IEinnsynRepository<Enhet, UUID> {
  public Enhet findByLegacyId(UUID id);
}
