// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.identifikator.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.features.validation.NoSSN;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;

@Getter
@Setter
public class IdentifikatorDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Identifikator";

  @Size(max = 500)
  @NoSSN
  private String navn;

  @Size(max = 500)
  @NoSSN
  private String identifikator;

  @Size(max = 500)
  @NoSSN
  private String initialer;

  @Size(max = 500)
  @NoSSN
  private String epostadresse;
}
