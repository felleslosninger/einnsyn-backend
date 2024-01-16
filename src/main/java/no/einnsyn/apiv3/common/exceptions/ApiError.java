package no.einnsyn.apiv3.common.exceptions;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ApiError {

  private HttpStatus status;
  private String message;
  private List<String> errors;
  private List<FieldValidationError> fieldErrors;

  public ApiError(
      final HttpStatus status,
      final String message,
      final List<String> errors,
      final List<FieldValidationError> fieldErrors) {
    super();
    this.status = status;
    this.message = message;
    this.errors = errors;
    this.fieldErrors = fieldErrors;
  }
}
