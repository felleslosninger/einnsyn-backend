// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.korrespondansepart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
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
public class KorrespondansepartController {
  private final KorrespondansepartService service;

  public KorrespondansepartController(KorrespondansepartService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/korrespondansepart")
  public ResponseEntity<ListResponseBody<KorrespondansepartDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KorrespondansepartService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = KorrespondansepartService.class)
          @NotNull
          KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }
}
