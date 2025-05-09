package no.einnsyn.backend.authentication.bruker;

import no.einnsyn.backend.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.backend.entities.bruker.BrukerService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BrukerUserDetailsService implements UserDetailsService {

  private final BrukerService brukerService;

  public BrukerUserDetailsService(BrukerService brukerService) {
    this.brukerService = brukerService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var bruker = brukerService.findByIdOrThrow(username, UsernameNotFoundException.class);
    return new BrukerUserDetails(bruker);
  }
}
