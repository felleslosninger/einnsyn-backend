package no.einnsyn.backend.authentication;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class EInnsynAuthentication implements Authentication {

  private final EInnsynPrincipal principal;
  private final Object credentials;
  private final Collection<? extends GrantedAuthority> authorities;
  private Object details;

  @Setter private boolean authenticated;

  public EInnsynAuthentication(
      EInnsynPrincipal principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities) {
    this.principal = principal;
    this.credentials = credentials;
    this.authorities = authorities;
  }

  public EInnsynAuthentication(Object credentials) {
    this(null, credentials, null);
  }

  @Override
  public String getName() {
    return principal.getName();
  }
}
