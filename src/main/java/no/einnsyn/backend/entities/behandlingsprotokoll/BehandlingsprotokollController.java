// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.behandlingsprotokoll;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
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
public class BehandlingsprotokollController {
  private final BehandlingsprotokollService service;

  public BehandlingsprotokollController(BehandlingsprotokollService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/behandlingsprotokoll")
  public ResponseEntity<PaginatedList<BehandlingsprotokollDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/behandlingsprotokoll/{id}")
  public ResponseEntity<BehandlingsprotokollDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = BehandlingsprotokollService.class)
          @NotNull
          BehandlingsprotokollDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }
}
