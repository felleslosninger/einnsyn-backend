package no.einnsyn.backend.entities.behandlingsprotokoll.models;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;

@Getter
@Setter
@Entity
public class Behandlingsprotokoll extends ArkivBase {
  private String tekstInnhold;

  private String tekstFormat;
}
