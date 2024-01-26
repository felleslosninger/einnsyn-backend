// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingDTO;
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

@SuppressWarnings("java:S1130")
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

  @GetMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id,
      @RequestBody @Validated(Update.class) TilbakemeldingDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
