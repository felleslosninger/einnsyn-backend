// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsak.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;

@Getter
@Setter
public class LagretSakListQueryDTO extends BaseListQueryDTO {

  @Size(max = 500)
  @NoSSN
  String bruker;
}
