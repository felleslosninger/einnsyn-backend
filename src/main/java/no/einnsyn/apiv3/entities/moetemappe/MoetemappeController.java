// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class MoetemappeController {

  private final MoetemappeService service;

  public MoetemappeController(MoetemappeService service) {
    this.service = service;
  }

  @GetMapping("/moetemappe")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe")
  public ResponseEntity<MoetemappeDTO> add(
    @Valid @RequestBody Moetemappe body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.add(body, query);
      var location = URI.create("/moetemappe/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @RequestBody Moetemappe body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<ResultListDTO> getMoetedokumentList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getMoetedokumentList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.getMoetedokumentList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<MoetedokumentDTO> addMoetedokument(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @RequestBody Moetedokument body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addMoetedokument(id, body, query);
      var location = URI.create("/moetedokument/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.addMoetedokument", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}/moetedokument/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetedokumentFromMoetemappe(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetedokumentService.class
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.removeMoetedokumentFromMoetemappe(
        id,
        subId,
        query
      );
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error(
        "Error executing MoetemappeService.removeMoetedokumentFromMoetemappe",
        e
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<ResultListDTO> getMoetesakList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getMoetesakList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.getMoetesakList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<MoetesakDTO> addMoetesak(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @RequestBody Moetesak body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addMoetesak(id, body, query);
      var location = URI.create("/moetesak/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.addMoetesak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}/moetesak/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetesakFromMoetemappe(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetesakService.class
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.removeMoetesakFromMoetemappe(id, subId, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error(
        "Error executing MoetemappeService.removeMoetesakFromMoetemappe",
        e
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
