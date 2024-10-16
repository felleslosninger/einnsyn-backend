package no.einnsyn.apiv3.authentication.apikey;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import no.einnsyn.apiv3.entities.apikey.models.ApiKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class ApiKeyUserDetails implements UserDetails {

  private final String id;
  private final String username;
  private final String password;
  private final String enhetId;
  private final Collection<? extends GrantedAuthority> authorities;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  private final boolean enabled;

  public static final GrantedAuthority enhetAuthority = () -> "ROLE_ENHET";
  public static final GrantedAuthority adminAuthority = () -> "ROLE_ADMIN";

  public ApiKeyUserDetails(ApiKey apiKey) {
    id = apiKey.getId();
    username = apiKey.getId();
    password = apiKey.getSecret();
    enhetId = apiKey.getEnhet().getId();
    accountNonExpired = true;
    accountNonLocked = true;
    credentialsNonExpired = true;
    enabled = true;
    authorities =
        apiKey.getEnhet().getParent() == null // Only top node doesn't have a parent
            ? List.of(enhetAuthority, adminAuthority)
            : List.of(enhetAuthority);
  }
}
