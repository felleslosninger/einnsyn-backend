// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.lagretsoek;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
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
public class LagretSoekController {
  private final LagretSoekService service;

  public LagretSoekController(LagretSoekService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/lagretSoek")
  public ResponseEntity<PaginatedList<LagretSoekDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          ExpandableField<LagretSoekDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          ExpandableField<LagretSoekDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/lagretSoek/{id}")
  public ResponseEntity<LagretSoekDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = LagretSoekService.class, mustExist = true)
          ExpandableField<LagretSoekDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = LagretSoekService.class)
          @NotNull
          LagretSoekDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }
}
