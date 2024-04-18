// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.lagretsak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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
public class LagretSakController {

  private final LagretSakService service;

  public LagretSakController(LagretSakService service) {
    this.service = service;
  }

  @GetMapping("/lagretSak")
  public ResponseEntity<ResultList<LagretSakDTO>> list(@Valid LagretSakListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/lagretSak/{lagretSakId}")
  public ResponseEntity<LagretSakDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class)
          String lagretSakId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(lagretSakId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/lagretSak/{lagretSakId}")
  public ResponseEntity<LagretSakDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class)
          String lagretSakId,
      @RequestBody @Validated(Update.class) LagretSakDTO body)
      throws EInnsynException {
    var responseBody = service.update(lagretSakId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/lagretSak/{lagretSakId}")
  public ResponseEntity<LagretSakDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class)
          String lagretSakId)
      throws EInnsynException {
    var responseBody = service.delete(lagretSakId);
    return ResponseEntity.ok().body(responseBody);
  }
}
