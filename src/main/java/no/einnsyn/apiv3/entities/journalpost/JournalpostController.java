// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartListQueryDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JournalpostController {

  private final JournalpostService service;

  public JournalpostController(JournalpostService service) {
    this.service = service;
  }

  @GetMapping("/journalpost/{journalpostId}")
  public ResponseEntity<JournalpostDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(journalpostId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/journalpost/{journalpostId}")
  public ResponseEntity<JournalpostDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @RequestBody @Validated(Update.class) JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.update(journalpostId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/journalpost/{journalpostId}")
  public ResponseEntity<JournalpostDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId)
      throws EInnsynException {
    var responseBody = service.delete(journalpostId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{journalpostId}/korrespondansepart")
  public ResponseEntity<ResultList<KorrespondansepartDTO>> getKorrespondansepartList(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @Valid KorrespondansepartListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKorrespondansepartList(journalpostId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{journalpostId}/korrespondansepart")
  public ResponseEntity<KorrespondansepartDTO> addKorrespondansepart(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @RequestBody @Validated(Insert.class) KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.addKorrespondansepart(journalpostId, body);
    var location = URI.create("/korrespondansepart/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/journalpost/{journalpostId}/dokumentbeskrivelse")
  public ResponseEntity<ResultList<DokumentbeskrivelseDTO>> getDokumentbeskrivelseList(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @Valid DokumentbeskrivelseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getDokumentbeskrivelseList(journalpostId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{journalpostId}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @RequestBody @Validated(Insert.class) DokumentbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.addDokumentbeskrivelse(journalpostId, body);
    var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/journalpost/{journalpostId}/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<JournalpostDTO> deleteDokumentbeskrivelse(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String journalpostId,
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String dokumentbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.deleteDokumentbeskrivelse(journalpostId, dokumentbeskrivelseId);
    return ResponseEntity.ok().body(responseBody);
  }
}
