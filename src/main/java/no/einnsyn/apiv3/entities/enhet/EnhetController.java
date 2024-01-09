// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class EnhetController {

  private final EnhetService service;

  public EnhetController(EnhetService service) {
    this.service = service;
  }

  @GetMapping("/enhet")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/enhet")
  public ResponseEntity<EnhetDTO> add(
    @Valid @RequestBody Enhet body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.add(body, query);
      var location = URI.create("/enhet/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/enhet/{id}/underenhet")
  public ResponseEntity<ResultListDTO> getUnderenhetList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getUnderenhetList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.getUnderenhetList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/enhet/{id}/underenhet")
  public ResponseEntity<EnhetDTO> addUnderenhet(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid @RequestBody Enhet body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addUnderenhet(id, body, query);
      var location = URI.create("/enhet/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.addUnderenhet", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/enhet/{id}/underenhet/{subId}")
  public ResponseEntity<EnhetDTO> deleteUnderenhet(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = EnhetService.class
    ) String id,
    @Valid @PathVariable @NotNull String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.deleteUnderenhet(id, subId, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.deleteUnderenhet", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
