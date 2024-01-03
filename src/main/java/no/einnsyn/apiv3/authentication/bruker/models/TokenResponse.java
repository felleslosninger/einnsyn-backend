package no.einnsyn.apiv3.authentication.bruker.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponse {
  private String token;
  private String refreshToken;
  private long expiresIn;

  public TokenResponse(String token, String refreshToken, long expiresIn) {
    this.token = token;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
  }
}
