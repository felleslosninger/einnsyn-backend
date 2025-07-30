// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.identifikator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
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

  /** List all objects. */
  @GetMapping("/identifikator")
  public ResponseEntity<PaginatedList<IdentifikatorDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          ExpandableField<IdentifikatorDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          ExpandableField<IdentifikatorDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/identifikator/{id}")
  public ResponseEntity<IdentifikatorDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = IdentifikatorService.class, mustExist = true)
          ExpandableField<IdentifikatorDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = IdentifikatorService.class)
          @NotNull
          IdentifikatorDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }
}
