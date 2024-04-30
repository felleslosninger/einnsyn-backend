package no.einnsyn.apiv3.entities.registrering.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class RegistreringES extends ArkivBaseES {
  private String offentligTittel;
  private String publisertDato;
  private String oppdatertDato;

  @SuppressWarnings("java:S116")
  private String offentligTittel_SENSITIV;
}
