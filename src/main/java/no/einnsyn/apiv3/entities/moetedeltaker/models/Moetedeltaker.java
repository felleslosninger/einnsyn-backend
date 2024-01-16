package no.einnsyn.apiv3.entities.moetedeltaker.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Moetedeltaker extends ArkivBase {
  private String moetedeltakerNavn;

  private String moetedeltakerFunksjon;
}
