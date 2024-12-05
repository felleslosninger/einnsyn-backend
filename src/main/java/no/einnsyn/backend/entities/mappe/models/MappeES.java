package no.einnsyn.backend.entities.mappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.backend.entities.registrering.models.RegistreringES;

@Getter
@Setter
public class MappeES extends ArkivBaseES {
  private String offentligTittel;
  private String publisertDato;
  private String oppdatertDato;

  @SuppressWarnings("java:S116")
  private String offentligTittel_SENSITIV;

  private List<RegistreringES> child;
}
