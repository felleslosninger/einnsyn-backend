// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.utredning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.utredning.models.ListByUtredningParameters;
import no.einnsyn.backend.entities.utredning.models.UtredningDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
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
public class UtredningController {
  private final UtredningService service;

  public UtredningController(UtredningService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/utredning")
  public ResponseEntity<PaginatedList<UtredningDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/utredning/{id}")
  public ResponseEntity<UtredningDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/utredning/{id}")
  public ResponseEntity<UtredningDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/utredning/{id}")
  public ResponseEntity<UtredningDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = UtredningService.class)
          @NotNull
          UtredningDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/utredning/{id}/utredningsdokument")
  public ResponseEntity<PaginatedList<DokumentbeskrivelseDTO>> listUtredningsdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id,
      @Valid ListByUtredningParameters query)
      throws EInnsynException {
    var responseBody = service.listUtredningsdokument(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/utredning/{id}/utredningsdokument")
  public ResponseEntity<DokumentbeskrivelseDTO> addUtredningsdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id,
      @RequestBody @Valid @NotNull ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    var responseBody = service.addUtredningsdokument(id, body);
    if (body.getId() == null) {
      var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }

  @DeleteMapping("/utredning/{id}/utredningsdokument/{utredningsdokumentId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteUtredningsdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = UtredningService.class, mustExist = true)
          String id,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String utredningsdokumentId)
      throws EInnsynException {
    var responseBody = service.deleteUtredningsdokument(id, utredningsdokumentId);
    return ResponseEntity.ok().body(responseBody);
  }
}
