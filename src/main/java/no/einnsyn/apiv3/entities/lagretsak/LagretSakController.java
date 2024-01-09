// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsak;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class LagretSakController {

  private final LagretSakService service;

  public LagretSakController(LagretSakService service) {
    this.service = service;
  }

  @GetMapping("/lagretSak/{id}")
  public ResponseEntity<LagretSakDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = LagretSakService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/lagretSak/{id}")
  public ResponseEntity<LagretSakDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = LagretSakService.class
    ) String id,
    @Valid @RequestBody LagretSak body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/lagretSak/{id}")
  public ResponseEntity<LagretSakDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = LagretSakService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
