// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.bruker.BrukerService;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.resultlist.models.ResultListDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
public class BrukerController {

  private final BrukerService service;

  public BrukerController(BrukerService service) {
    this.service = service;
  }

  @GetMapping("/bruker")
  public ResponseEntity<ResultListDTO> list(
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker")
  public ResponseEntity<BrukerDTO> add(
    @Valid @RequestBody Bruker body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.add(body, query);
      var location = URI.create("/bruker/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid QueryParametersDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.update(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.delete(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<ResultListDTO> getInnsynskravList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getInnsynskravList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.getInnsynskravList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<InnsynskravDTO> addInnsynskrav(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @RequestBody Innsynskrav body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addInnsynskrav(id, body, query);
      var location = URI.create("/innsynskrav/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addInnsynskrav", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/innsynskrav/{subId}")
  public ResponseEntity<BrukerDTO> deleteInnsynskrav(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = InnsynskravService.class
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.deleteInnsynskrav(id, subId, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteInnsynskrav", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<ResultListDTO> getLagretSoekList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getLagretSoekList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.getLagretSoekList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<LagretSoekDTO> addLagretSoek(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @RequestBody LagretSoek body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addLagretSoek(id, body, query);
      var location = URI.create("/lagretsoek/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addLagretSoek", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/lagretSoek/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSoek(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = LagretSoekService.class
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.deleteLagretSoek(id, subId, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteLagretSoek", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<ResultListDTO> getLagretSakList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid ListQueryParametersDTO query
  ) {
    try {
      var responseBody = service.getLagretSakList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.getLagretSakList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<LagretSakDTO> addLagretSak(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @RequestBody LagretSak body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.addLagretSak(id, body, query);
      var location = URI.create("/lagretsak/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addLagretSak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/lagretSak/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSak(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = SaksmappeService.class
    ) String subId,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.deleteLagretSak(id, subId, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteLagretSak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker/{id}/activate/{secret}")
  public ResponseEntity<BrukerDTO> activate(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @PathVariable @NotNull String secret,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.activate(id, secret, query);
      var location = URI.create("//" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.activate", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/updatePassword")
  public ResponseEntity<BrukerDTO> updatePassword(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @RequestBody PutBrukerPasswordRequestBody body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.updatePassword(id, body, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.updatePassword", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/updatePassword/{secret}")
  public ResponseEntity<BrukerDTO> updatePasswordWithSecret(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid @PathVariable @NotNull String secret,
    @Valid @RequestBody PutBrukerPasswordWithSecretRequestBody body,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.updatePasswordWithSecret(
        id,
        secret,
        body,
        query
      );
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.updatePasswordWithSecret", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker/{id}/requestPasswordReset")
  public ResponseEntity<BrukerDTO> requestPasswordReset(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = BrukerService.class
    ) String id,
    @Valid EmptyQueryDTO query
  ) {
    try {
      var responseBody = service.requestPasswordReset(id, query);
      var location = URI.create("//" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.requestPasswordReset", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
