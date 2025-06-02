package no.einnsyn.backend.authentication;

import java.security.Principal;
import lombok.Getter;

@Getter
public class EInnsynPrincipal implements Principal {

  // Ansattporten, ApiKey or Bruker
  private final String authType;

  // ID representing the authentication, if any (ApiKey etc.)
  private final String authId;

  // ID for the authenticated entity
  private final String id;

  // "Human" id. Orgnummer or email.
  private final String name;

  // Is the authenticated user an admin?
  private final boolean admin;

  public EInnsynPrincipal(String authType, String authId, String id, String name, boolean admin) {
    this.authType = authType;
    this.authId = authId;
    this.id = id;
    this.name = name;
    this.admin = admin;
  }
}
