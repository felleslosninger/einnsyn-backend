package no.einnsyn.apiv3.authorization;

import no.einnsyn.apiv3.authentication.bruker.models.BrukerUserDetails;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("preAuth")
public class PreAuthorizeFunctions {

  BrukerService brukerService;

  public PreAuthorizeFunctions(BrukerService brukerService) {
    this.brukerService = brukerService;
  }

  public boolean isSelf(String username) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var authenticationName = authentication.getName();
    var authorities = authentication.getAuthorities();

    // Logged in as a "bruker"
    if (authorities.contains(BrukerUserDetails.brukerAuthority)) {
      // If the given param is an email, check equality
      if (authenticationName.equals(username)) {
        return true;
      }

      // The given param might be an id, "name" is always an email for "bruker"
      var brukerFromParam = brukerService.findById(username);
      if (brukerFromParam != null && brukerFromParam.getEmail().equals(authenticationName)) {
        return true;
      }
    }

    return false;
  }
}
