// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.skjerming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
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
public class SkjermingController {
  private final SkjermingService service;

  public SkjermingController(SkjermingService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/skjerming")
  public ResponseEntity<ListResponseBody<SkjermingDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/skjerming")
  public ResponseEntity<SkjermingDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = SkjermingService.class, mustNotExist = true)
          @NotNull
          SkjermingDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/skjerming/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/skjerming/{id}")
  public ResponseEntity<SkjermingDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = SkjermingService.class)
          @NotNull
          SkjermingDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }
}
