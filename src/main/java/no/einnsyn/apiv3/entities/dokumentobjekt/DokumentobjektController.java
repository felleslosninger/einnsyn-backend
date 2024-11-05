// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentobjekt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DokumentobjektController {

  private final DokumentobjektService service;

  public DokumentobjektController(DokumentobjektService service) {
    this.service = service;
  }

  @GetMapping("/dokumentobjekt")
  public ResponseEntity<ResultList<DokumentobjektDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/dokumentobjekt/{dokumentobjektId}")
  public ResponseEntity<DokumentobjektDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          String dokumentobjektId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(dokumentobjektId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/dokumentobjekt/{dokumentobjektId}")
  public ResponseEntity<DokumentobjektDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          String dokumentobjektId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = DokumentobjektService.class)
          DokumentobjektDTO body)
      throws EInnsynException {
    var responseBody = service.update(dokumentobjektId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/dokumentobjekt/{dokumentobjektId}")
  public ResponseEntity<DokumentobjektDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentobjektService.class, mustExist = true)
          String dokumentobjektId)
      throws EInnsynException {
    var responseBody = service.delete(dokumentobjektId);
    return ResponseEntity.ok().body(responseBody);
  }
}
