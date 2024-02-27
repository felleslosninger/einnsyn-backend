package no.einnsyn.apiv3.error.responses;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ErrorResponse {

  private HttpStatus status;
  private String message;
  private List<String> errors;
  private List<FieldValidationError> fieldErrors;

  public ErrorResponse(
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
