// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class InternalServerErrorException extends EInnsynException {

  public InternalServerErrorException(String message, Throwable cause) {
    super(message, cause, "internalServerError");
  }

  public InternalServerErrorException(String message) {
    super(message, "internalServerError");
  }

  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getMessage());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected final String type = "internalServerError";

    protected String message;

    public ClientResponse(String message) {
      super();
      this.message = message;
    }
  }
}
