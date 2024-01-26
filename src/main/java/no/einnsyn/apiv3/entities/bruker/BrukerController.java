// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.bruker.models.BrukerDTO;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravService;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsak.LagretSakService;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.apiv3.entities.lagretsak.models.LagretSakListQueryDTO;
import no.einnsyn.apiv3.entities.lagretsoek.LagretSoekService;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.password.Password;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("java:S1130")
@RestController
public class BrukerController {

  private final BrukerService service;

  public BrukerController(BrukerService service) {
    this.service = service;
  }

  @GetMapping("/bruker")
  public ResponseEntity<ResultList<BrukerDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker")
  public ResponseEntity<BrukerDTO> add(@RequestBody @Validated(Insert.class) BrukerDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/bruker/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @RequestBody @Validated(Update.class) BrukerDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<ResultList<InnsynskravDTO>> getInnsynskravList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid InnsynskravListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getInnsynskravList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<InnsynskravDTO> addInnsynskrav(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @RequestBody @Validated(Insert.class) InnsynskravDTO body)
      throws EInnsynException {
    var responseBody = service.addInnsynskrav(id, body);
    var location = URI.create("/innsynskrav/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/bruker/{id}/innsynskrav/{subId}")
  public ResponseEntity<BrukerDTO> deleteInnsynskrav(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.deleteInnsynskrav(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<ResultList<LagretSoekDTO>> getLagretSoekList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid LagretSoekListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getLagretSoekList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<LagretSoekDTO> addLagretSoek(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @RequestBody @Validated(Insert.class) LagretSoekDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSoek(id, body);
    var location = URI.create("/lagretsoek/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/bruker/{id}/lagretSoek/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSoek(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSoekService.class) String subId)
      throws EInnsynException {
    var responseBody = service.deleteLagretSoek(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<ResultList<LagretSakDTO>> getLagretSakList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid LagretSakListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getLagretSakList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<LagretSakDTO> addLagretSak(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @RequestBody @Validated(Insert.class) LagretSakDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSak(id, body);
    var location = URI.create("/lagretsak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/bruker/{id}/lagretSak/{subId}")
  public ResponseEntity<BrukerDTO> deleteLagretSak(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = LagretSakService.class) String subId)
      throws EInnsynException {
    var responseBody = service.deleteLagretSak(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{id}/activate/{secret}")
  public ResponseEntity<BrukerDTO> activate(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.activate(id, secret);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{id}/updatePassword")
  public ResponseEntity<BrukerDTO> updatePassword(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @RequestBody @Validated(Update.class) PutBrukerPasswordDTO body)
      throws EInnsynException {
    var responseBody = service.updatePassword(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{id}/updatePassword/{secret}")
  public ResponseEntity<BrukerDTO> updatePasswordWithSecret(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id,
      @Valid @PathVariable @NotNull String secret,
      @RequestBody @Validated(Update.class) PutBrukerPasswordWithSecretDTO body)
      throws EInnsynException {
    var responseBody = service.updatePasswordWithSecret(id, secret, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{id}/requestPasswordReset")
  public ResponseEntity<BrukerDTO> requestPasswordReset(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String id)
      throws EInnsynException {
    var responseBody = service.requestPasswordReset(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @Getter
  @Setter
  public class PutBrukerPasswordDTO {

    @Size(max = 500)
    @NoSSN
    String oldPassword;

    @Size(max = 500)
    @Password
    String newPassword;
  }

  @Getter
  @Setter
  public class PutBrukerPasswordWithSecretDTO {

    @Size(max = 500)
    @Password
    String newPassword;
  }
}
