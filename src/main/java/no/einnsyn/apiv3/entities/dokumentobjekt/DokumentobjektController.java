// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentobjekt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
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
public class DokumentobjektController {

  private final DokumentobjektService service;

  public DokumentobjektController(DokumentobjektService service) {
    this.service = service;
  }

  @GetMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentobjektService.class)
          String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentobjektService.class)
          String id,
      @RequestBody @Validated(Update.class) DokumentobjektDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentobjektService.class)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
