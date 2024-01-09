// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.saksmappe;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class SaksmappeController {

  private final SaksmappeService service;

  public SaksmappeController(SaksmappeService service) {
    this.service = service;
  }

  @GetMapping("/saksmappe")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
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
    @Valid @RequestBody Saksmappe body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.add(body, query);
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
    @Valid QueryParametersDTO query
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
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, query);
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
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing SaksmappeService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<ResultListDTO> getJournalpostList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String id,
    @Valid ListQueryParametersDTO query
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
    @Valid @RequestBody Journalpost body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addJournalpost(id, body, query);
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
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.removeJournalpostFromSaksmappe(
        id,
        subId,
        query
      );
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
