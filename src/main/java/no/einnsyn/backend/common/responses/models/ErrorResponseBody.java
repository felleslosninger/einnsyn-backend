// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.responses.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class ErrorResponseBody {
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String status;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String message;
}
