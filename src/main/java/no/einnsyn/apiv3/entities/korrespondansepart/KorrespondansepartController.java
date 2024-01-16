// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class KorrespondansepartController {

  private final KorrespondansepartService service;

  public KorrespondansepartController(KorrespondansepartService service) {
    this.service = service;
  }

  @GetMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing KorrespondansepartService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String id,
      @Valid @RequestBody KorrespondansepartDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing KorrespondansepartService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing KorrespondansepartService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
