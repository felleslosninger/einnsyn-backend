package no.einnsyn.apiv3.authentication.hmac;

import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class HmacUserDetails implements UserDetails {

  private String username;
  private String password;
  private Collection<? extends GrantedAuthority> authorities;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
  private boolean enabled;

  // public HmacUserDetails(ApiKeyRecord apiKeyRecord) {
  // username = bruker.getEmail();
  // password = bruker.getPassword();
  // authorities = List.of();
  // accountNonExpired = true;
  // accountNonLocked = true;
  // credentialsNonExpired = true;
  // enabled = bruker.isActive();
  // }

}
