// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.responses.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class ValidationErrorResponseBody extends ErrorResponseBody {
  @NotNull(groups = {Insert.class})
  List<FieldError> fieldError;

  @Getter
  @Setter
  public class FieldError {
    @NoSSN
    @Size(max = 500)
    @NotBlank(groups = {Insert.class})
    String fieldName;

    @NoSSN
    @Size(max = 500)
    String value;

    @NoSSN
    @Size(max = 500)
    String message;
  }
}
