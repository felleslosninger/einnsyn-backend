// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.votering;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.votering.VoteringService;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class VoteringController {

  private final VoteringService service;

  public VoteringController(VoteringService service) {
    this.service = service;
  }

  @GetMapping("/votering/{id}")
  public ResponseEntity<VoteringDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VoteringService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VoteringService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/votering/{id}")
  public ResponseEntity<VoteringDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VoteringService.class
    ) String id,
    @Valid @RequestBody Votering body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VoteringService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/votering/{id}")
  public ResponseEntity<VoteringDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VoteringService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VoteringService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
