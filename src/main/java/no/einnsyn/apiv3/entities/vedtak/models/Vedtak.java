package no.einnsyn.apiv3.entities.vedtak.models;

import jakarta.persistence.Entity;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Vedtak extends ArkivBase {
  // private Moetesaksbeskrivelse vedtakstekst;

  // private List<VoteringDTO> votering;

  // private Behandlingsprotokoll behandlingsprotokoll;

  // private List<DokumentbeskrivelseDTO> vedtaksdokumenter;

  private Date dato;
}
