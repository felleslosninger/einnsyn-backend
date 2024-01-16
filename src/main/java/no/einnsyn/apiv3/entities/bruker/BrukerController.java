// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
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
public class BrukerController {

  private final BrukerService service;

  public BrukerController(BrukerService service) {
    this.service = service;
  }

  @GetMapping("/bruker")
  public ResponseEntity<ResultList<BrukerDTO>> list(@Valid BaseListQueryDTO query) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/bruker")
  public ResponseEntity<BrukerDTO> add(@Valid @RequestBody BrukerDTO body) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/bruker/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid BaseGetQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @RequestBody BrukerDTO body) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<ResultList<InnsynskravDTO>> getInnsynskravList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid InnsynskravListQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @RequestBody InnsynskravDTO body) {
    try {
      var responseBody = service.addInnsynskrav(id, body);
      var location = URI.create("/innsynskrav/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addInnsynskrav", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/innsynskrav/{subId}")
  public ResponseEntity<BrukerDTO> deleteInnsynskrav(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String subId) {
    try {
      var responseBody = service.deleteInnsynskrav(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteInnsynskrav", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<ResultList<LagretSoekDTO>> getLagretSoekList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid LagretSoekListQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @RequestBody LagretSoekDTO body) {
    try {
      var responseBody = service.addLagretSoek(id, body);
      var location = URI.create("/lagretsoek/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addLagretSoek", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/lagretSoek/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSoek(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String subId) {
    try {
      var responseBody = service.deleteLagretSoek(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteLagretSoek", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<ResultList<LagretSakDTO>> getLagretSakList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid LagretSakListQueryDTO query) {
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
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @RequestBody LagretSakDTO body) {
    try {
      var responseBody = service.addLagretSak(id, body);
      var location = URI.create("/lagretsak/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.addLagretSak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/bruker/{id}/lagretSak/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSak(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String subId) {
    try {
      var responseBody = service.deleteLagretSak(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.deleteLagretSak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/activate/{secret}")
  public ResponseEntity<BrukerDTO> activate(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String secret) {
    try {
      var responseBody = service.activate(id, secret);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.activate", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/updatePassword")
  public ResponseEntity<BrukerDTO> updatePassword(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @RequestBody PutBrukerPasswordDTO body) {
    try {
      var responseBody = service.updatePassword(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.updatePassword", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/updatePassword/{secret}")
  public ResponseEntity<BrukerDTO> updatePasswordWithSecret(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String secret,
      @Valid @RequestBody PutBrukerPasswordWithSecretDTO body) {
    try {
      var responseBody = service.updatePasswordWithSecret(id, secret, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.updatePasswordWithSecret", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/bruker/{id}/requestPasswordReset")
  public ResponseEntity<BrukerDTO> requestPasswordReset(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id) {
    try {
      var responseBody = service.requestPasswordReset(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing BrukerService.requestPasswordReset", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @Getter
  @Setter
  public class PutBrukerPasswordDTO {

    @Size(max = 500)
    String oldPassword;

    @Size(min = 8, max = 500)
    String newPassword;
  }

  @Getter
  @Setter
  public class PutBrukerPasswordWithSecretDTO {

    @Size(min = 8, max = 500)
    String newPassword;
  }
}
