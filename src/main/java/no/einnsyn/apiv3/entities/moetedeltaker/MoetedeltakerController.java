// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedeltaker;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class MoetedeltakerController {

  private final MoetedeltakerService service;

  public MoetedeltakerController(MoetedeltakerService service) {
    this.service = service;
  }

  @GetMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetedeltakerService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetedeltakerService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetedeltakerService.class
    ) String id,
    @Valid @RequestBody Moetedeltaker body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetedeltakerService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetedeltakerService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetedeltakerService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
