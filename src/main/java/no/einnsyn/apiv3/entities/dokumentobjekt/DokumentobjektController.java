// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.dokumentobjekt;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.dokumentobjekt.DokumentobjektService;
import no.einnsyn.apiv3.entities.dokumentobjekt.models.DokumentobjektDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class DokumentobjektController {

  private final DokumentobjektService service;

  public DokumentobjektController(DokumentobjektService service) {
    this.service = service;
  }

  @GetMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = DokumentobjektService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentobjektService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = DokumentobjektService.class
    ) String id,
    @Valid @RequestBody Dokumentobjekt body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentobjektService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/dokumentobjekt/{id}")
  public ResponseEntity<DokumentobjektDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = DokumentobjektService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing DokumentobjektService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
