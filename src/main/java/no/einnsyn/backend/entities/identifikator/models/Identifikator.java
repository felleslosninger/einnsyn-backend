package no.einnsyn.backend.entities.identifikator.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Identifikator extends ArkivBase {

  private String navn;

  @SuppressWarnings("java:S1700")
  private String identifikator;

  private String initialer;

  private String epostadresse;
}
