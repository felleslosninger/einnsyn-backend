// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.klassifikasjonssystem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.ListByKlassifikasjonssystemParameters;
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
public class KlassifikasjonssystemController {
  private final KlassifikasjonssystemService service;

  public KlassifikasjonssystemController(KlassifikasjonssystemService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/klassifikasjonssystem")
  public ResponseEntity<PaginatedList<KlassifikasjonssystemDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = KlassifikasjonssystemService.class)
          @NotNull
          KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klassifikasjonssystem/{id}/klasse")
  public ResponseEntity<PaginatedList<KlasseDTO>> listKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String id,
      @Valid ListByKlassifikasjonssystemParameters query)
      throws EInnsynException {
    var responseBody = service.listKlasse(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klassifikasjonssystem/{id}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = KlasseService.class, mustNotExist = true)
          @NotNull
          KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(id, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
