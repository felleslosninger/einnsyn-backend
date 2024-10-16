package no.einnsyn.apiv3.entities.saksmappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.mappe.models.MappeES;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class SaksmappeES extends MappeES {
  private String saksaar;
  private String sakssekvensnummer;
  private String saksnummer;
  private List<String> saksnummerGenerert;
  private String standardDato;

  @SuppressWarnings("java:S2065")
  private transient String saksdato;

  @Getter
  @Setter
  public static class SaksmappeWithoutChildrenES extends SaksmappeES {
    @SuppressWarnings("java:S2065")
    private transient List<RegistreringES> child;
  }
}
