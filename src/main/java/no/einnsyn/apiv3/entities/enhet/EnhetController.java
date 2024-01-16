// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetListQueryDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EnhetController {

  private final EnhetService service;

  public EnhetController(EnhetService service) {
    this.service = service;
  }

  @GetMapping("/enhet")
  public ResponseEntity<ResultList<EnhetDTO>> list(@Valid EnhetListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/enhet")
  public ResponseEntity<EnhetDTO> add(@Valid @RequestBody EnhetDTO body) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/enhet/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid BaseGetQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid @RequestBody EnhetDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/enhet/{id}/underenhet")
  public ResponseEntity<ResultList<EnhetDTO>> getUnderenhetList(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid EnhetListQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid @RequestBody EnhetDTO body) {
    try {
      var responseBody = service.addUnderenhet(id, body);
      var location = URI.create("/enhet/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.addUnderenhet", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/enhet/{id}/underenhet/{subId}")
  public ResponseEntity<EnhetDTO> deleteUnderenhet(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid @PathVariable @NotNull String subId) {
    try {
      var responseBody = service.deleteUnderenhet(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing EnhetService.deleteUnderenhet", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
