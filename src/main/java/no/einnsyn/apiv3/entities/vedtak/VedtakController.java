// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.vedtak;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.vedtak.VedtakService;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class VedtakController {

  private final VedtakService service;

  public VedtakController(VedtakService service) {
    this.service = service;
  }

  @GetMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VedtakService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VedtakService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VedtakService.class
    ) String id,
    @Valid @RequestBody Vedtak body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VedtakService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = VedtakService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing VedtakService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
