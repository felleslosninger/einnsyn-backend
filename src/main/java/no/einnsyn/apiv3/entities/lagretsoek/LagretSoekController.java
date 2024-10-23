// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsoek;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
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
public class LagretSoekController {

  private final LagretSoekService service;

  public LagretSoekController(LagretSoekService service) {
    this.service = service;
  }

  @GetMapping("/lagretSoek")
  public ResponseEntity<ResultList<LagretSoekDTO>> list(@Valid LagretSoekListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/lagretSoek/{lagretSoekId}")
  public ResponseEntity<LagretSoekDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          String lagretSoekId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(lagretSoekId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/lagretSoek/{lagretSoekId}")
  public ResponseEntity<LagretSoekDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          String lagretSoekId,
      @RequestBody @Validated(Update.class) LagretSoekDTO body)
      throws EInnsynException {
    var responseBody = service.update(lagretSoekId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/lagretSoek/{lagretSoekId}")
  public ResponseEntity<LagretSoekDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          String lagretSoekId)
      throws EInnsynException {
    var responseBody = service.delete(lagretSoekId);
    return ResponseEntity.ok().body(responseBody);
  }
}
