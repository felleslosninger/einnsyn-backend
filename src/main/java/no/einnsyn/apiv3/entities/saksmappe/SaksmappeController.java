// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.saksmappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;
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
public class SaksmappeController {

  private final SaksmappeService service;

  public SaksmappeController(SaksmappeService service) {
    this.service = service;
  }

  @GetMapping("/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> list(
    @Valid BaseListQueryDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/saksmappe")
  public ResponseEntity<SaksmappeDTO> add(
    @RequestBody @Validated(Insert.class) SaksmappeDTO body
  ) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/saksmappe/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @Valid BaseGetQueryDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @RequestBody @Validated(Update.class) SaksmappeDTO body
  ) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id
  ) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<ResultList<JournalpostDTO>> getJournalpostList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @Valid JournalpostListQueryDTO query
  ) {
    try {
      var responseBody = service.getJournalpostList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.getJournalpostList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<JournalpostDTO> addJournalpost(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @RequestBody @Validated(Insert.class) JournalpostDTO body
  ) {
    try {
      var responseBody = service.addJournalpost(id, body);
      var location = URI.create("/journalpost/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.addJournalpost", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/saksmappe/{id}/journalpost/{subId}")
  public ResponseEntity<SaksmappeDTO> removeJournalpostFromSaksmappe(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = JournalpostService.class
    ) String subId
  ) {
    try {
      var responseBody = service.removeJournalpostFromSaksmappe(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error(
        "Error executing SaksmappeService.removeJournalpostFromSaksmappe",
        e
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
