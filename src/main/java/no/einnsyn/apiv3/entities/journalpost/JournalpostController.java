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
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
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

  @GetMapping("/journalpost")
  public ResponseEntity<ResultList<JournalpostDTO>> list(@Valid JournalpostListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @RequestBody @Validated(Update.class) JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<ResultList<KorrespondansepartDTO>> getKorrespondansepartList(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid KorrespondansepartListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKorrespondansepartList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<KorrespondansepartDTO> addKorrespondansepart(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @RequestBody @Validated(Insert.class) KorrespondansepartDTO body)
      throws EInnsynException {
    var responseBody = service.addKorrespondansepart(id, body);
    var location = URI.create("/korrespondansepart/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/journalpost/{id}/korrespondansepart/{subId}")
  public ResponseEntity<JournalpostDTO> removeKorrespondansepartFromJournalpost(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = KorrespondansepartService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.removeKorrespondansepartFromJournalpost(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/journalpost/{id}/dokumentbeskrivelse")
  public ResponseEntity<ResultList<DokumentbeskrivelseDTO>> getDokumentbeskrivelseList(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid DokumentbeskrivelseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getDokumentbeskrivelseList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/journalpost/{id}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @RequestBody @Validated(Insert.class) DokumentbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.addDokumentbeskrivelse(id, body);
    var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/journalpost/{id}/dokumentbeskrivelse/{subId}")
  public ResponseEntity<JournalpostDTO> removeDokumentbeskrivelseFromJournalpost(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.removeDokumentbeskrivelseFromJournalpost(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }
}
