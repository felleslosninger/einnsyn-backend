// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.journalpost;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.backend.entities.korrespondansepart.models.KorrespondansepartDTO;
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
public class JournalpostController {
  private final JournalpostService service;

  public JournalpostController(JournalpostService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/journalpost")
  public ResponseEntity<PaginatedList<JournalpostDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = JournalpostService.class)
          @NotNull
          JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{id}/dokumentbeskrivelse")
  public ResponseEntity<PaginatedList<DokumentbeskrivelseDTO>> listDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @Valid ListByJournalpostParameters query)
      throws EInnsynException {
    var responseBody = service.listDokumentbeskrivelse(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{id}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @RequestBody @Valid @NotNull ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    var responseBody = service.addDokumentbeskrivelse(id.getId(), body);
    if (body.getId() == null) {
      var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }

  @DeleteMapping("/journalpost/{id}/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.deleteDokumentbeskrivelse(id.getId(), dokumentbeskrivelseId.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<PaginatedList<KorrespondansepartDTO>> listKorrespondansepart(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @Valid ListByJournalpostParameters query)
      throws EInnsynException {
    var responseBody = service.listKorrespondansepart(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<KorrespondansepartDTO> addKorrespondansepart(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = JournalpostService.class, mustExist = true)
          ExpandableField<JournalpostDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = KorrespondansepartService.class, mustNotExist = true)
          @NotNull
          KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.addKorrespondansepart(id.getId(), body);
    var location = URI.create("/korrespondansepart/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
