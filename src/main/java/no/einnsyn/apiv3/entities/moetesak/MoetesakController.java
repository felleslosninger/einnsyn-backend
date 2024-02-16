// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
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
public class MoetesakController {

  private final MoetesakService service;

  public MoetesakController(MoetesakService service) {
    this.service = service;
  }

  @GetMapping("/moetesak/{moetesakId}")
  public ResponseEntity<MoetesakDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesakService.class)
          String moetesakId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetesakId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetesak/{moetesakId}")
  public ResponseEntity<MoetesakDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesakService.class)
          String moetesakId,
      @RequestBody @Validated(Update.class) MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetesakId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetesak/{moetesakId}")
  public ResponseEntity<MoetesakDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesakService.class)
          String moetesakId)
      throws EInnsynException {
    var responseBody = service.delete(moetesakId);
    return ResponseEntity.ok().body(responseBody);
  }
}
