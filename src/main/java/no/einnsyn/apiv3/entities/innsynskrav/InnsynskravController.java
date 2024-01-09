// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class InnsynskravController {

  private final InnsynskravService service;

  public InnsynskravController(InnsynskravService service) {
    this.service = service;
  }

  @GetMapping("/innsynskrav")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/innsynskrav/{id}/")
  public ResponseEntity<InnsynskravDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/innsynskrav/{id}/")
  public ResponseEntity<InnsynskravDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravService.class
    ) String id,
    @Valid @RequestBody Innsynskrav body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/innsynskrav/{id}/")
  public ResponseEntity<InnsynskravDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
