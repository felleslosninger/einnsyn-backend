package no.einnsyn.apiv3.authentication.bruker.models;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;

public class BrukerLoginRequestBody {

  @Getter
  private String username;

  @Getter
  private String password;

  @Getter
  private String refreshToken;

  @AssertTrue(message = "Either username and password or refresh token must be provided")
  public boolean isPassword() {
    return hasAuthentication();
  }

  @AssertTrue(message = "Either username and password or refresh token must be provided")
  public boolean isRefreshToken() {
    return hasAuthentication();
  }

  @AssertTrue(message = "Either username and password or refresh token must be provided")
  public boolean isUsername() {
    return hasAuthentication();
  }

  private boolean hasAuthentication() {
    return (username != null && !username.isBlank() && password != null && !password.isBlank())
        || (refreshToken != null && !refreshToken.isBlank());
  }
}
