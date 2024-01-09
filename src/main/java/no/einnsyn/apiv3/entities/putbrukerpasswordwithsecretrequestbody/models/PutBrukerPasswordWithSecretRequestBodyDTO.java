// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.putbrukerpasswordwithsecretrequestbody.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;

@Getter
@Setter
public class PutBrukerPasswordWithSecretRequestBodyDTO {

  @Size(max = 500)
  @NoSSN
  private String newPassword;
}
