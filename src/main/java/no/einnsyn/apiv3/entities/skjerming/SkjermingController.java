// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.skjerming;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.skjerming.SkjermingService;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class SkjermingController {

  private final SkjermingService service;

  public SkjermingController(SkjermingService service) {
    this.service = service;
  }

  @GetMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SkjermingService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SkjermingService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SkjermingService.class
    ) String id,
    @Valid @RequestBody Skjerming body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SkjermingService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SkjermingService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SkjermingService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
