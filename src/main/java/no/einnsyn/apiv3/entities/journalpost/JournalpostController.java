// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class JournalpostController {

  private final JournalpostService service;

  public JournalpostController(JournalpostService service) {
    this.service = service;
  }

  @GetMapping("/journalpost")
  public ResponseEntity<ResultList<JournalpostDTO>> list(@Valid JournalpostListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/journalpost")
  public ResponseEntity<JournalpostDTO> add(
      @RequestBody @Validated(Insert.class) JournalpostDTO body) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/journalpost/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @RequestBody @Validated(Update.class) JournalpostDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<ResultList<KorrespondansepartDTO>> getKorrespondansepartList(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid BaseListQueryDTO query) {
    try {
      var responseBody = service.getKorrespondansepartList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.getKorrespondansepartList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<KorrespondansepartDTO> addKorrespondansepart(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @RequestBody @Validated(Insert.class) KorrespondansepartDTO body) {
    try {
      var responseBody = service.addKorrespondansepart(id, body);
      var location = URI.create("/korrespondansepart/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.addKorrespondansepart", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<KorrespondansepartDTO> deleteKorrespondansepart(
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class) String id,
      @Valid @PathVariable @NotNull String subId) {
    try {
      var responseBody = service.deleteKorrespondansepart(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.deleteKorrespondansepart", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
