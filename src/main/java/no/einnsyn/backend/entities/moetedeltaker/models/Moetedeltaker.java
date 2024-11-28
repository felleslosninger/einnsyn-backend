package no.einnsyn.backend.entities.moetedeltaker.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Moetedeltaker extends ArkivBase {
  private String moetedeltakerNavn;

  private String moetedeltakerFunksjon;
}
