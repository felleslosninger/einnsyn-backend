// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.behandlingsprotokoll;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.BehandlingsprotokollService;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class BehandlingsprotokollController {

  private final BehandlingsprotokollService service;

  public BehandlingsprotokollController(BehandlingsprotokollService service) {
    this.service = service;
  }

  @GetMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BehandlingsprotokollService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BehandlingsprotokollService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BehandlingsprotokollService.class
    ) String id,
    @Valid @RequestBody Behandlingsprotokoll body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BehandlingsprotokollService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BehandlingsprotokollService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BehandlingsprotokollService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
