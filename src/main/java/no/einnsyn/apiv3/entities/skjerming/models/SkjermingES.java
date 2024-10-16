package no.einnsyn.apiv3.entities.skjerming.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseES;

@Getter
@Setter
public class SkjermingES extends ArkivBaseES {
  private String tilgangsrestriksjon;
  private String skjermingshjemmel;
}
