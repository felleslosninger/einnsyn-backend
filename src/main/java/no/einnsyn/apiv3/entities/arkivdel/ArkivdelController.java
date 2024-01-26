// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkivdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
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
public class ArkivdelController {

  private final ArkivdelService service;

  public ArkivdelController(ArkivdelService service) {
    this.service = service;
  }

  @GetMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Update.class) ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
