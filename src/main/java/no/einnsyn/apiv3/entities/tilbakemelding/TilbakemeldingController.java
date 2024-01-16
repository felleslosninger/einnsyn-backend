// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingDTO;
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
public class TilbakemeldingController {

  private final TilbakemeldingService service;

  public TilbakemeldingController(TilbakemeldingService service) {
    this.service = service;
  }

  @GetMapping("/tilbakemelding")
  public ResponseEntity<ResultList<TilbakemeldingDTO>> list(@Valid BaseListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing TilbakemeldingService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/tilbakemelding")
  public ResponseEntity<TilbakemeldingDTO> add(@Valid @RequestBody TilbakemeldingDTO body) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/tilbakemelding/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing TilbakemeldingService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id,
      @Valid BaseGetQueryDTO query) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing TilbakemeldingService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id,
      @Valid @RequestBody TilbakemeldingDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing TilbakemeldingService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = TilbakemeldingService.class)
          String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing TilbakemeldingService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
