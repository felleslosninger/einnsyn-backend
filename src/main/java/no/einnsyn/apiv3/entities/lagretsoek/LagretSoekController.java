// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsoek;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
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
public class LagretSoekController {

  private final LagretSoekService service;

  public LagretSoekController(LagretSoekService service) {
    this.service = service;
  }

  @GetMapping("/lagretSoek")
  public ResponseEntity<ResultList<LagretSoekDTO>> list(@Valid LagretSoekListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSoekService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSoekService.class) String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSoekService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSoekService.class) String id,
      @RequestBody @Validated(Update.class) LagretSoekDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSoekService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSoekService.class) String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing LagretSoekService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
