// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LagretSakController {

  private final LagretSakService service;

  public LagretSakController(LagretSakService service) {
    this.service = service;
  }

  @GetMapping("/lagretSak")
  public ResponseEntity<ResultList<LagretSakDTO>> list(@Valid LagretSakListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/lagretSak/{id}")
  public ResponseEntity<LagretSakDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class) String id,
      @Valid BaseGetQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class) String id,
      @RequestBody @Validated(Update.class) LagretSakDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/lagretSak/{id}")
  public ResponseEntity<LagretSakDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class) String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSakService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
