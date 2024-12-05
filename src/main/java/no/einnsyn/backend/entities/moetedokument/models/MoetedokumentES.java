package no.einnsyn.backend.entities.moetedokument.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MoetedokumentES extends RegistreringES {
  @SuppressWarnings("java:S116")
  private String møtedokumentregistreringstype;

  private MoetemappeWithoutChildrenES parent;
  private List<DokumentbeskrivelseES> dokumentbeskrivelse;
}
