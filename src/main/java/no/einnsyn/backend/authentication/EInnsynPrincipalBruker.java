package no.einnsyn.backend.authentication;

public class EInnsynPrincipalBruker extends EInnsynPrincipal {

  public EInnsynPrincipalBruker(
      String authType, String authId, String id, String name, boolean admin) {
    super(authType, authId, id, name, admin);
  }
}
