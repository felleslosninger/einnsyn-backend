// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.saksmappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
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

  @GetMapping("/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/saksmappe")
  public ResponseEntity<SaksmappeDTO> add(@RequestBody @Validated(Insert.class) SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id,
      @RequestBody @Validated(Update.class) SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<ResultList<JournalpostDTO>> getJournalpostList(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id,
      @Valid JournalpostListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getJournalpostList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<JournalpostDTO> addJournalpost(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id,
      @RequestBody @Validated(Insert.class) JournalpostDTO body)
      throws EInnsynException {
    var responseBody = service.addJournalpost(id, body);
    var location = URI.create("/journalpost/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/saksmappe/{id}/journalpost/{subId}")
  public ResponseEntity<SaksmappeDTO> deleteJournalpost(
      @Valid @PathVariable @NotNull @ExistingObject(service = SaksmappeService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = JournalpostService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.deleteJournalpost(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }
}
