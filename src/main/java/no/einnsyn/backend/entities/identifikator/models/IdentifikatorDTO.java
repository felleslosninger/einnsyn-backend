// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.identifikator.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;

@Getter
@Setter
public class IdentifikatorDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Identifikator";

  @Size(max = 500)
  @NoSSN
  String navn;

  @Size(max = 500)
  @NoSSN
  String identifikator;

  @Size(max = 500)
  @NoSSN
  String initialer;

  @Size(max = 500)
  @NoSSN
  String epostadresse;
}
