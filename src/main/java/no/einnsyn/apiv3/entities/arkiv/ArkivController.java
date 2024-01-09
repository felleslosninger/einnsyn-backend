// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkiv;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.arkiv.ArkivService;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class ArkivController {

  private final ArkivService service;

  public ArkivController(ArkivService service) {
    this.service = service;
  }

  @GetMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = ArkivService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing ArkivService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = ArkivService.class
    ) String id,
    @Valid @RequestBody Arkiv body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing ArkivService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = ArkivService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing ArkivService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
