// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class BadRequestException extends EInnsynException {

  public BadRequestException(String message, Throwable cause) {
    super(message, cause, "badRequest");
  }

  public BadRequestException(String message) {
    super(message, "badRequest");
  }

  @Override
  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getMessage());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected final String type = "badRequest";

    protected String message;

    public ClientResponse(String message) {
      super();
      this.message = message;
    }
  }
}
