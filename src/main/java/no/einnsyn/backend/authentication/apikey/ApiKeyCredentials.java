package no.einnsyn.backend.authentication.apikey;

import lombok.Getter;

@Getter
public class ApiKeyCredentials {
  private String apiKey;
  private String actingAs;

  public ApiKeyCredentials(String apiKey, String actingAs) {
    this.apiKey = apiKey;
    this.actingAs = actingAs;
  }
}
