package no.einnsyn.apiv3.responses;

import java.util.List;
import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {

  private HttpStatus status;
  private String message;
  private List<String> errors;
  private List<FieldValidationError> fieldErrors;

  public ApiError(final HttpStatus status, final String message, final List<String> errors,
      final List<FieldValidationError> fieldErrors) {
    super();
    this.status = status;
    this.message = message;
    this.errors = errors;
    this.fieldErrors = fieldErrors;
  }
}
