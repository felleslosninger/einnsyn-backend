package no.einnsyn.backend.entities.arkivbase.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseES;

@Getter
@Setter
public class ArkivBaseES extends BaseES {
  // Legacy - arkivskaper is the IRI of administrativEnhetObjekt
  private String arkivskaper;
  private List<String> arkivskaperNavn;
  private String arkivskaperSorteringNavn;
  private List<String> arkivskaperTransitive;
}
