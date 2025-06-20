// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.common.authinfo;

import no.einnsyn.backend.common.authinfo.models.AuthInfoResponse;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthInfoController {
  private final AuthInfoService service;

  public AuthInfoController(AuthInfoService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public ResponseEntity<AuthInfoResponse> get() throws EInnsynException {
    var responseBody = service.get();
    return ResponseEntity.ok().body(responseBody);
  }
}
