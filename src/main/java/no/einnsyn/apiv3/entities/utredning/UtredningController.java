// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.utredning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UtredningController {

  private final UtredningService service;

  public UtredningController(UtredningService service) {
    this.service = service;
  }

  @GetMapping("/utredning/{utredningId}")
  public ResponseEntity<UtredningDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = UtredningService.class)
          String utredningId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(utredningId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/utredning/{utredningId}")
  public ResponseEntity<UtredningDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = UtredningService.class)
          String utredningId,
      @RequestBody @Validated(Update.class) UtredningDTO body)
      throws EInnsynException {
    var responseBody = service.update(utredningId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/utredning/{utredningId}")
  public ResponseEntity<UtredningDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = UtredningService.class)
          String utredningId)
      throws EInnsynException {
    var responseBody = service.delete(utredningId);
    return ResponseEntity.ok().body(responseBody);
  }
}
