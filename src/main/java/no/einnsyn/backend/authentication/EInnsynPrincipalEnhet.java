package no.einnsyn.backend.authentication;

public class EInnsynPrincipalEnhet extends EInnsynPrincipal {

  public EInnsynPrincipalEnhet(
      String authType, String authId, String id, String name, boolean admin) {
    super(authType, authId, id, name, admin);
  }
}
