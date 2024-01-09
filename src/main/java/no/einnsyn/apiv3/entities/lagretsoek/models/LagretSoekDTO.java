// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsoek.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class LagretSoekDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "LagretSoek";

  @Size(max = 500)
  @NoSSN
  private String query;
}
