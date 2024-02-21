package no.einnsyn.apiv3.entities.vedtak;

import no.einnsyn.apiv3.entities.arkivbase.ArkivBaseRepository;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.vedtak.models.Vedtak;
import org.springframework.data.jpa.repository.Query;

public interface VedtakRepository extends ArkivBaseRepository<Vedtak> {

  @Query(
      "SELECT COUNT(v) FROM Vedtak v JOIN v.vedtaksdokument d WHERE d =" + " :dokumentbeskrivelse")
  int countByVedtaksdokument(Dokumentbeskrivelse dokumentbeskrivelse);
}
