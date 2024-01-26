package no.einnsyn.apiv3.entities.votering.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Votering extends ArkivBase {
  private final String entity = "Votering";

  // private MoetedeltakerDTO moetedeltaker;

  private StemmeEnum stemme;

  // private IdentifikatorDTO representerer;
}
