// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.tilbakemelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.tilbakemelding.models.TilbakemeldingDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TilbakemeldingController {
  private final TilbakemeldingService service;

  public TilbakemeldingController(TilbakemeldingService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/tilbakemelding")
  public ResponseEntity<PaginatedList<TilbakemeldingDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/tilbakemelding")
  public ResponseEntity<TilbakemeldingDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = TilbakemeldingService.class, mustNotExist = true)
          @NotNull
          TilbakemeldingDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/tilbakemelding/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = TilbakemeldingService.class, mustExist = true)
          ExpandableField<TilbakemeldingDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = TilbakemeldingService.class, mustExist = true)
          ExpandableField<TilbakemeldingDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = TilbakemeldingService.class, mustExist = true)
          ExpandableField<TilbakemeldingDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = TilbakemeldingService.class)
          @NotNull
          TilbakemeldingDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }
}
