package no.einnsyn.apiv3.entities.tilbakemelding;

import no.einnsyn.apiv3.entities.EinnsynRepository;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.tilbakemelding.models.Tilbakemelding;

import java.util.UUID;

public interface TilbakemeldingRepository extends EinnsynRepository<Tilbakemelding, Long> {
}
