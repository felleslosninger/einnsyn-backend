// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravdel;

import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class InnsynskravDelController {

  private final InnsynskravDelService service;

  public InnsynskravDelController(InnsynskravDelService service) {
    this.service = service;
  }

  @GetMapping("/innsynskravDel/{id}")
  public ResponseEntity<InnsynskravDelDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravDelService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravDelService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/innsynskravDel/{id}")
  public ResponseEntity<InnsynskravDelDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravDelService.class
    ) String id,
    @Valid @RequestBody InnsynskravDel body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravDelService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/innsynskravDel/{id}")
  public ResponseEntity<InnsynskravDelDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravDelService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing InnsynskravDelService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
