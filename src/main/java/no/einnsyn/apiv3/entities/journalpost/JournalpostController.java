// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.journalpost;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class JournalpostController {

  private final JournalpostService service;

  public JournalpostController(JournalpostService service) {
    this.service = service;
  }

  @GetMapping("/journalpost")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
  ) {
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
    @Valid @RequestBody Journalpost body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.add(body, query);
      var location = URI.create("/journalpost/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = JournalpostService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
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
    @Valid @PathVariable @NotNull @ExistingObject(
      service = JournalpostService.class
    ) String id,
    @Valid @RequestBody Journalpost body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = JournalpostService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing JournalpostService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
