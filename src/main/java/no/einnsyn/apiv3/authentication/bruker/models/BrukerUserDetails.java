package no.einnsyn.apiv3.authentication.bruker.models;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.Getter;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;

@Getter
public class BrukerUserDetails implements UserDetails {

  private String id;
  private String username;
  private String password;
  private Collection<? extends GrantedAuthority> authorities;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
  private boolean enabled;

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
