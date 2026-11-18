package no.einnsyn.backend.authentication;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

  // Every Enhet the caller has access to: the authorized orgs and their underenheter.
  // The set is for hasEnhetAccess, which runs once per loaded object.
  private final List<String> enhetIdList;
  private final Set<String> enhetIdSet;

  @Setter private boolean authenticated;

  public EInnsynAuthentication(
      EInnsynPrincipal principal,
      Object credentials,
      Collection<? extends GrantedAuthority> authorities) {
    this.principal = principal;
    this.credentials = credentials;
    this.authorities = authorities == null ? List.of() : authorities;

    var enhetIds = new LinkedHashSet<String>();
    for (var authority : this.authorities) {
      if (authority instanceof EInnsynAuthority enhetAuthority
          && "Enhet".equals(enhetAuthority.getEntity())
          && enhetAuthority.getId() != null
          && !enhetAuthority.getId().isBlank()) {
        enhetIds.add(enhetAuthority.getId());
      }
    }
    this.enhetIdList = List.copyOf(enhetIds);
    this.enhetIdSet = Set.copyOf(enhetIds);
  }

  public EInnsynAuthentication(Object credentials) {
    this(null, credentials, null);
  }

  @Override
  public String getName() {
    return principal.getName();
  }
}
