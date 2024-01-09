// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.identifikator;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.identifikator.IdentifikatorService;
import no.einnsyn.apiv3.entities.identifikator.models.IdentifikatorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class IdentifikatorController {

  private final IdentifikatorService service;

  public IdentifikatorController(IdentifikatorService service) {
    this.service = service;
  }

  @GetMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = IdentifikatorService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing IdentifikatorService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = IdentifikatorService.class
    ) String id,
    @Valid @RequestBody Identifikator body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing IdentifikatorService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = IdentifikatorService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing IdentifikatorService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
