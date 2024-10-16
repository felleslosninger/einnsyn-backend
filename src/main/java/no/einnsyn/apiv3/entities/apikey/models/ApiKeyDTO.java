// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.apikey.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class ApiKeyDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "ApiKey";

  @Size(max = 500)
  @NoSSN
  String name;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String secretKey;

  @Valid ExpandableField<EnhetDTO> enhet;
}
