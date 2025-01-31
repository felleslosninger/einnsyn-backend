package no.einnsyn.backend.authentication.apikey.models;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import no.einnsyn.backend.entities.apikey.models.ApiKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class ApiKeyUserDetails implements UserDetails {

  private final String id;
  private final String username;
  private final String password;
  private final String enhetId;
  private final List<String> enhetSubtreeIdList;
  private final Collection<? extends GrantedAuthority> authorities;
  private final boolean accountNonExpired;
  private final boolean accountNonLocked;
  private final boolean credentialsNonExpired;
  private final boolean enabled;

  public static final GrantedAuthority enhetAuthority = () -> "ROLE_ENHET";
  public static final GrantedAuthority adminAuthority = () -> "ROLE_ADMIN";

  public ApiKeyUserDetails(ApiKey apiKey, String enhetId, List<String> enhetSubtreeIdList) {
    id = apiKey.getId();
    username = apiKey.getId();
    password = apiKey.getSecret();
    this.enhetId = enhetId;
    this.enhetSubtreeIdList = enhetSubtreeIdList;
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
