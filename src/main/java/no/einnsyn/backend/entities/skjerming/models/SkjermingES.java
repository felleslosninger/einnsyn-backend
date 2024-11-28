package no.einnsyn.backend.entities.skjerming.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class SkjermingES extends ArkivBaseES {
  private String tilgangsrestriksjon;
  private String skjermingshjemmel;
}
