// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DokumentbeskrivelseController {

  private final DokumentbeskrivelseService service;

  public DokumentbeskrivelseController(DokumentbeskrivelseService service) {
    this.service = service;
  }

  @GetMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentbeskrivelseService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @Valid @RequestBody DokumentbeskrivelseDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentbeskrivelseService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentbeskrivelseService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/dokumentbeskrivelse/{id}/download/{subId}.{docExtension}")
  public ResponseEntity<byte[]> downloadDokumentbeskrivelse(
      @Valid @PathVariable @NotNull @ExistingObject(service = DokumentbeskrivelseService.class)
          String id,
      @Valid @PathVariable @NotNull String subId,
      @Valid @PathVariable @NotNull String docExtension) {
    try {
      var responseBody = service.downloadDokumentbeskrivelse(id, subId, docExtension);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentbeskrivelseService.downloadDokumentbeskrivelse", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
