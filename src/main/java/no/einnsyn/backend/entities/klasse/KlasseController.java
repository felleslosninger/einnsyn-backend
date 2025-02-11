// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.klasse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klasse.models.ListByKlasseParameters;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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
public class KlasseController {
  private final KlasseService service;

  public KlasseController(KlasseService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/klasse")
  public ResponseEntity<PaginatedList<KlasseDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = KlasseService.class)
          @NotNull
          KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klasse/{id}/klasse")
  public ResponseEntity<PaginatedList<KlasseDTO>> listKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id,
      @Valid ListByKlasseParameters query)
      throws EInnsynException {
    var responseBody = service.listKlasse(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klasse/{id}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
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

  @GetMapping("/klasse/{id}/moetemappe")
  public ResponseEntity<PaginatedList<MoetemappeDTO>> listMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id,
      @Valid ListByKlasseParameters query)
      throws EInnsynException {
    var responseBody = service.listMoetemappe(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klasse/{id}/saksmappe")
  public ResponseEntity<PaginatedList<SaksmappeDTO>> listSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlasseService.class, mustExist = true)
          String id,
      @Valid ListByKlasseParameters query)
      throws EInnsynException {
    var responseBody = service.listSaksmappe(id, query);
    return ResponseEntity.ok().body(responseBody);
  }
}
