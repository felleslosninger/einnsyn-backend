package no.einnsyn.apiv3.entities.mappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MappeES extends ArkivBaseES {
  private String offentligTittel;
  private String publisertDato;

  @SuppressWarnings("java:S116")
  private String offentligTittel_SENSITIV;

  private List<RegistreringES> child;
}
