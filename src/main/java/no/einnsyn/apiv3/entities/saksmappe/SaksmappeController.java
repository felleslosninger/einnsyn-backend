// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.saksmappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
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
public class SaksmappeController {

  private final SaksmappeService service;

  public SaksmappeController(SaksmappeService service) {
    this.service = service;
  }

  @GetMapping("/saksmappe/{saksmappeId}")
  public ResponseEntity<SaksmappeDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class)
          String saksmappeId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(saksmappeId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/saksmappe/{saksmappeId}")
  public ResponseEntity<SaksmappeDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class)
          String saksmappeId,
      @RequestBody @Validated(Update.class) SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(saksmappeId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/saksmappe/{saksmappeId}")
  public ResponseEntity<SaksmappeDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class)
          String saksmappeId)
      throws EInnsynException {
    var responseBody = service.delete(saksmappeId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/saksmappe/{saksmappeId}/journalpost")
  public ResponseEntity<ResultList<JournalpostDTO>> getJournalpostList(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class)
          String saksmappeId,
      @Valid JournalpostListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getJournalpostList(saksmappeId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/saksmappe/{saksmappeId}/journalpost")
  public ResponseEntity<JournalpostDTO> addJournalpost(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class)
          String saksmappeId,
      @RequestBody @Validated(Insert.class) JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.addJournalpost(saksmappeId, body);
    var location = URI.create("/journalpost/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
