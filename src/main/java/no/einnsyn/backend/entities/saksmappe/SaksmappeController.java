// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.saksmappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.journalpost.JournalpostService;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
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
public class SaksmappeController {
  private final SaksmappeService service;

  public SaksmappeController(SaksmappeService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/saksmappe")
  public ResponseEntity<PaginatedList<SaksmappeDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SaksmappeService.class, mustExist = true)
          ExpandableField<SaksmappeDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SaksmappeService.class, mustExist = true)
          ExpandableField<SaksmappeDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SaksmappeService.class, mustExist = true)
          ExpandableField<SaksmappeDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = SaksmappeService.class)
          @NotNull
          SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<PaginatedList<JournalpostDTO>> listJournalpost(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SaksmappeService.class, mustExist = true)
          ExpandableField<SaksmappeDTO> id,
      @Valid ListBySaksmappeParameters query)
      throws EInnsynException {
    var responseBody = service.listJournalpost(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<JournalpostDTO> addJournalpost(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SaksmappeService.class, mustExist = true)
          ExpandableField<SaksmappeDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = JournalpostService.class, mustNotExist = true)
          @NotNull
          JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.addJournalpost(id.getId(), body);
    var location = URI.create("/journalpost/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
