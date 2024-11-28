// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.dokumentbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.backend.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String dokumentbeskrivelseId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(dokumentbeskrivelseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String dokumentbeskrivelseId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = DokumentbeskrivelseService.class)
          DokumentbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.update(dokumentbeskrivelseId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String dokumentbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.delete(dokumentbeskrivelseId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/dokumentbeskrivelse/{dokumentbeskrivelseId}/download/{subId}.{docExtension}")
  public ResponseEntity<byte[]> downloadDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String dokumentbeskrivelseId,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          String subId,
      @Valid @PathVariable @NotNull String docExtension)
      throws EInnsynException {
    var responseBody =
        service.downloadDokumentbeskrivelse(dokumentbeskrivelseId, subId, docExtension);
    return ResponseEntity.ok().body(responseBody);
  }
}
