package no.einnsyn.apiv3.authentication.bruker;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;

@Service
public class BrukerUserDetailsService implements UserDetailsService {

  private final BrukerService brukerService;

  public BrukerUserDetailsService(BrukerService brukerService) {
    this.brukerService = brukerService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Bruker bruker = brukerService.findById(username);
    if (bruker == null) {
      throw new UsernameNotFoundException(username);
    }
    return new BrukerUserDetails(bruker);
  }
}
