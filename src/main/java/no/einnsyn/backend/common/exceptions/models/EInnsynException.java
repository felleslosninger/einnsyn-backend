// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class EInnsynException extends Exception {
  protected String type = "eInnsynException";

  public EInnsynException(String message, Throwable cause, String type) {
    super(message, cause);
    this.type = type;
  }

  public EInnsynException(String message, String type) {
    super(message);
    this.type = type;
  }

  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getType(), this.getMessage());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected String type = "eInnsynException";

    protected String message;

    public ClientResponse(String type, String message) {
      super();
      this.type = type;
      this.message = message;
    }
  }
}
