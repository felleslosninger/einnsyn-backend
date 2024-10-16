package no.einnsyn.apiv3.entities.moetedokument.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MoetedokumentES extends RegistreringES {
  @SuppressWarnings("java:S116")
  private String møtedokumentregistreringstype;

  private MoetemappeWithoutChildrenES parent;
  private List<DokumentbeskrivelseES> dokumentbeskrivelse;
}
