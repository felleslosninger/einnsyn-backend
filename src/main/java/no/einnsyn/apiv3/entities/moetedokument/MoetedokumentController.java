// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedokument;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
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
public class MoetedokumentController {

  private final MoetedokumentService service;

  public MoetedokumentController(MoetedokumentService service) {
    this.service = service;
  }

  @GetMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class)
          String moetedokumentId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetedokumentId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class)
          String moetedokumentId,
      @RequestBody @Validated(Update.class) MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetedokumentId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class)
          String moetedokumentId)
      throws EInnsynException {
    var responseBody = service.delete(moetedokumentId);
    return ResponseEntity.ok().body(responseBody);
  }
}
