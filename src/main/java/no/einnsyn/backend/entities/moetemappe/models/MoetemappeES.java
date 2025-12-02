package no.einnsyn.backend.entities.moetemappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.mappe.models.MappeES;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MoetemappeES extends MappeES {
  private String utvalg;
  private String moetested;
  private String moetedato;
  private String standardDato;
  private boolean fulltext = false;

  @Getter
  @Setter
  public static class MoetemappeWithoutChildrenES extends MoetemappeES {
    @SuppressWarnings("java:S2065")
    private transient List<RegistreringES> child;
  }
}
