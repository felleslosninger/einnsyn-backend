package no.einnsyn.backend.entities.registrering.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class RegistreringES extends ArkivBaseES {
  private String offentligTittel;
  private String publisertDato;
  private String oppdatertDato;

  private String statRelation = "registrering";

  @SuppressWarnings("java:S116")
  private String offentligTittel_SENSITIV;

  private String sorteringstype = "";
  private boolean fulltext = false;
}
