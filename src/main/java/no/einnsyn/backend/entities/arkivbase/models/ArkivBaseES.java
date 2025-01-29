package no.einnsyn.backend.entities.arkivbase.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseES;

@Getter
@Setter
public class ArkivBaseES extends BaseES {

  private String administrativEnhet;
  private List<String> administrativEnhetTransitive;

  // Legacy - arkivskaper is the IRI of administrativEnhetObjekt
  private String arkivskaper;
  private List<String> arkivskaperTransitive;

  private List<String> arkivskaperNavn;
  private String arkivskaperSorteringNavn;
}
