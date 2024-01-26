// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedeltaker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
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

@SuppressWarnings("java:S1130")
@RestController
public class MoetedeltakerController {

  private final MoetedeltakerService service;

  public MoetedeltakerController(MoetedeltakerService service) {
    this.service = service;
  }

  @GetMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedeltakerService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedeltakerService.class) String id,
      @RequestBody @Validated(Update.class) MoetedeltakerDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedeltakerService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
