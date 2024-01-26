// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedokument;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
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
public class MoetedokumentController {

  private final MoetedokumentService service;

  public MoetedokumentController(MoetedokumentService service) {
    this.service = service;
  }

  @GetMapping("/moetedokument")
  public ResponseEntity<ResultList<MoetedokumentDTO>> list(@Valid MoetedokumentListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class) String id,
      @RequestBody @Validated(Update.class) MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
