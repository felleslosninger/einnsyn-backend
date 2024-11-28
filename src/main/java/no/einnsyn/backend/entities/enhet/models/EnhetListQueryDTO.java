// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.enhet.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseListQueryDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;

@Getter
@Setter
public class EnhetListQueryDTO extends BaseListQueryDTO {

  @Size(max = 500)
  @NoSSN
  String parentId;
}
