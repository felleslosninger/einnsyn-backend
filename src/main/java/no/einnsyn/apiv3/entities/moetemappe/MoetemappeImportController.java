package no.einnsyn.apiv3.entities.moetemappe;

import jakarta.validation.Valid;
import java.net.URI;
import no.einnsyn.apiv3.authentication.AuthenticationService;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MoetemappeImportController {

  private final AuthenticationService authenticationService;
  private final MoetemappeService moetemappeService;

  public MoetemappeImportController(
      AuthenticationService authenticationService, MoetemappeService service) {
    this.authenticationService = authenticationService;
    this.moetemappeService = service;
  }

  @PostMapping("/moetemappe/import")
  public ResponseEntity<MoetemappeDTO> importMoetemappe(
      @RequestBody @Valid MoetemappeDTO moetemappeDTO) throws EInnsynException {

    if (!authenticationService.isAdmin()) {
      throw new ForbiddenException("Not authorized");
    }

    // Check if item already exists
    // var found = moetemappeService.findByDTO(moetemappeDTO);
    // if (found != null) {
    //   throw new EInnsynException("Conflict");
    // }

    var responseBody = moetemappeService.add(moetemappeDTO);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
