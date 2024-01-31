// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
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
public class DokumentbeskrivelseController {

  private final DokumentbeskrivelseService service;

  public DokumentbeskrivelseController(DokumentbeskrivelseService service) {
    this.service = service;
  }

  @GetMapping("/dokumentbeskrivelse")
  public ResponseEntity<ResultList<DokumentbeskrivelseDTO>> list(
      @Valid DokumentbeskrivelseListQueryDTO query) throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @RequestBody @Validated(Update.class) DokumentbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/dokumentbeskrivelse/{id}/download/{subId}.{docExtension}")
  public ResponseEntity<byte[]> downloadDokumentbeskrivelse(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentobjektService.class)
          String subId,
      @Valid @PathVariable @NotNull String docExtension)
      throws EInnsynException {
    var responseBody = service.downloadDokumentbeskrivelse(id, subId, docExtension);
    return ResponseEntity.ok().body(responseBody);
  }
}
