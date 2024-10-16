package no.einnsyn.apiv3.entities.moetesak.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeES.MoetemappeWithoutChildrenES;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MoetesakES extends RegistreringES {

  @SuppressWarnings("java:S116")
  private List<String> referanseTilMøtesak;

  @SuppressWarnings("java:S116")
  private String møtesaksår;

  @SuppressWarnings("java:S116")
  private String møtesakssekvensnummer;

  private MoetemappeWithoutChildrenES parent;
  private List<DokumentbeskrivelseES> dokumentbeskrivelse;
  private String saksnummer;
  private List<String> saksnummerGenerert;
  private String standardDato;
  private String moetedato;
  private String utvalg;
}
