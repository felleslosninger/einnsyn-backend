// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.identifikator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.backend.entities.base.models.BaseListQueryDTO;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
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
public class IdentifikatorController {

  private final IdentifikatorService service;

  public IdentifikatorController(IdentifikatorService service) {
    this.service = service;
  }

  @GetMapping("/identifikator")
  public ResponseEntity<ResultList<IdentifikatorDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/identifikator/{identifikatorId}")
  public ResponseEntity<IdentifikatorDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          String identifikatorId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(identifikatorId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/identifikator/{identifikatorId}")
  public ResponseEntity<IdentifikatorDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          String identifikatorId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = IdentifikatorService.class)
          IdentifikatorDTO body)
      throws EInnsynException {
    var responseBody = service.update(identifikatorId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/identifikator/{identifikatorId}")
  public ResponseEntity<IdentifikatorDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          String identifikatorId)
      throws EInnsynException {
    var responseBody = service.delete(identifikatorId);
    return ResponseEntity.ok().body(responseBody);
  }
}
