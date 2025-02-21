// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetedeltaker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.moetedeltaker.models.MoetedeltakerDTO;
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
public class MoetedeltakerController {
  private final MoetedeltakerService service;

  public MoetedeltakerController(MoetedeltakerService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/moetedeltaker")
  public ResponseEntity<PaginatedList<MoetedeltakerDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/moetedeltaker/{id}")
  public ResponseEntity<MoetedeltakerDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = MoetedeltakerService.class)
          @NotNull
          MoetedeltakerDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }
}
