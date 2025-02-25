// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class MethodNotAllowedException extends EInnsynException {

  public MethodNotAllowedException(String message, Throwable cause) {
    super(message, cause, "methodNotAllowed");
  }

  public MethodNotAllowedException(String message) {
    super(message, "methodNotAllowed");
  }

  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getMessage());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected final String type = "methodNotAllowed";

    protected String message;

    public ClientResponse(String message) {
      super();
      this.message = message;
    }
  }
}
