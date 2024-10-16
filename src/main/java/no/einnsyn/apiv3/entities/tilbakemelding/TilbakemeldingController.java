// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TilbakemeldingController {

  private final TilbakemeldingService service;

  public TilbakemeldingController(TilbakemeldingService service) {
    this.service = service;
  }

  @GetMapping("/tilbakemelding")
  public ResponseEntity<ResultList<TilbakemeldingDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/tilbakemelding")
  public ResponseEntity<TilbakemeldingDTO> add(
      @RequestBody @Validated(Insert.class) TilbakemeldingDTO body) throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/tilbakemelding/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/tilbakemelding/{tilbakemeldingId}")
  public ResponseEntity<TilbakemeldingDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String tilbakemeldingId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(tilbakemeldingId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/tilbakemelding/{tilbakemeldingId}")
  public ResponseEntity<TilbakemeldingDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String tilbakemeldingId,
      @RequestBody @Validated(Update.class) TilbakemeldingDTO body)
      throws EInnsynException {
    var responseBody = service.update(tilbakemeldingId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/tilbakemelding/{tilbakemeldingId}")
  public ResponseEntity<TilbakemeldingDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String tilbakemeldingId)
      throws EInnsynException {
    var responseBody = service.delete(tilbakemeldingId);
    return ResponseEntity.ok().body(responseBody);
  }
}
