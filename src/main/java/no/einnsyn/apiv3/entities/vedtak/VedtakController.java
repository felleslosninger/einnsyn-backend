// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.vedtak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
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
public class VedtakController {

  private final VedtakService service;

  public VedtakController(VedtakService service) {
    this.service = service;
  }

  @GetMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = VedtakService.class) String vedtakId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(vedtakId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = VedtakService.class) String vedtakId,
      @RequestBody @Validated(Update.class) VedtakDTO body)
      throws EInnsynException {
    var responseBody = service.update(vedtakId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = VedtakService.class) String vedtakId)
      throws EInnsynException {
    var responseBody = service.delete(vedtakId);
    return ResponseEntity.ok().body(responseBody);
  }
}
