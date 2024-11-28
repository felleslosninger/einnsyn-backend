package no.einnsyn.backend.authentication.bruker.models;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import no.einnsyn.backend.entities.bruker.models.Bruker;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class BrukerUserDetails implements UserDetails {

  private final String id;
  private final String username;
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  private final boolean enabled;

  public static final GrantedAuthority brukerAuthority = () -> "ROLE_BRUKER";

  public BrukerUserDetails(Bruker bruker) {
    id = bruker.getId();
    username = bruker.getEmail();
    password = bruker.getPassword();
    authorities = List.of(brukerAuthority);
    accountNonExpired = true;
    accountNonLocked = true;
    credentialsNonExpired = true;
    enabled = bruker.isActive();
  }
}
