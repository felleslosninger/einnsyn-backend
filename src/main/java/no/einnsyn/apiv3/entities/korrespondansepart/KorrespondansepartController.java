// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
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
public class KorrespondansepartController {

  private final KorrespondansepartService service;

  public KorrespondansepartController(KorrespondansepartService service) {
    this.service = service;
  }

  @GetMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String korrespondansepartId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(korrespondansepartId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String korrespondansepartId,
      @RequestBody @Validated(Update.class) KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.update(korrespondansepartId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String korrespondansepartId)
      throws EInnsynException {
    var responseBody = service.delete(korrespondansepartId);
    return ResponseEntity.ok().body(responseBody);
  }
}
