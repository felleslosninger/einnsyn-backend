package no.einnsyn.apiv3.authentication.bruker.models;

import lombok.Getter;

@Getter
public class BrukerLoginRequestBody {
  private String username;
  private String password;
  private String refreshToken;
}
