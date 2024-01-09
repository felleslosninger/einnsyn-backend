// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.putbrukerpasswordrequestbody.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.features.validation.NoSSN;

@Getter
@Setter
public class PutBrukerPasswordRequestBodyDTO {

  @Size(max = 500)
  @NoSSN
  private String oldPassword;

  @Size(max = 500)
  @NoSSN
  private String newPassword;
}
