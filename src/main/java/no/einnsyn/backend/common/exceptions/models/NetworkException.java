// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.exceptions.models;

import lombok.Getter;
import no.einnsyn.backend.common.responses.models.ErrorResponse;

@Getter
public class NetworkException extends EInnsynException {
  protected String baseUrl;

  public NetworkException(String message, Throwable cause, String baseUrl) {
    super(message, cause, "networkError");
    this.baseUrl = baseUrl;
  }

  public NetworkException(String message) {
    super(message, "networkError");
  }

  public ErrorResponse toClientResponse() {
    return new ClientResponse(this.getMessage(), this.getBaseUrl());
  }

  @Getter
  public static class ClientResponse implements ErrorResponse {
    protected final String type = "networkError";

    protected String message;

    protected String baseUrl;

    public ClientResponse(String message, String baseUrl) {
      super();
      this.message = message;
      this.baseUrl = baseUrl;
    }
  }
}
