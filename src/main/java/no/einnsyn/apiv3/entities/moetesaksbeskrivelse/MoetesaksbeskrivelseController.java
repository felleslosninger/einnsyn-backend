// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
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
public class MoetesaksbeskrivelseController {

  private final MoetesaksbeskrivelseService service;

  public MoetesaksbeskrivelseController(MoetesaksbeskrivelseService service) {
    this.service = service;
  }

  @GetMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetesaksbeskrivelseService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id,
      @RequestBody @Validated(Update.class) MoetesaksbeskrivelseDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetesaksbeskrivelseService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetesaksbeskrivelseService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
