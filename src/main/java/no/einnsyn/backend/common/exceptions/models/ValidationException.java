// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import java.util.List;
import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class ValidationException extends EInnsynException {
  protected List<FieldError> fieldError;

  public ValidationException(String message, Throwable cause, List<FieldError> fieldError) {
    super(message, cause, "validationError");
    this.fieldError = fieldError;
  }

  public ValidationException(String message, List<FieldError> fieldError) {
    super(message, "validationError");
    this.fieldError = fieldError;
  }

  @Override
  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getMessage(), this.getFieldError());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected final String type = "validationError";

    protected String message;

    protected List<FieldError> fieldError;

    public ClientResponse(String message, List<FieldError> fieldError) {
      super();
      this.message = message;
      this.fieldError = fieldError;
    }
  }

  @Getter
  public static class FieldError {
    protected String fieldName;

    protected String value;

    protected String message;

    public FieldError(String fieldName, String value, String message) {
      super();
      this.fieldName = fieldName;
      this.value = value;
      this.message = message;
    }
  }
}
