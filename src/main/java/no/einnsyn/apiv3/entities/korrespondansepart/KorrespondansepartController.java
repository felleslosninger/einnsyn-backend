// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
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
public class KorrespondansepartController {

  private final KorrespondansepartService service;

  public KorrespondansepartController(KorrespondansepartService service) {
    this.service = service;
  }

  @GetMapping("/korrespondansepart")
  public ResponseEntity<ResultList<KorrespondansepartDTO>> list(
      @Valid KorrespondansepartListQueryDTO query) throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String korrespondansepartId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(korrespondansepartId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String korrespondansepartId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = KorrespondansepartService.class)
          KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.update(korrespondansepartId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/korrespondansepart/{korrespondansepartId}")
  public ResponseEntity<KorrespondansepartDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String korrespondansepartId)
      throws EInnsynException {
    var responseBody = service.delete(korrespondansepartId);
    return ResponseEntity.ok().body(responseBody);
  }
}
