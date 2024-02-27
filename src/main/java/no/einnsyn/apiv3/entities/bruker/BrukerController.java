// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.bruker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
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
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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

  @GetMapping("/bruker/{brukerId}")
  public ResponseEntity<BrukerDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(brukerId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{brukerId}")
  public ResponseEntity<BrukerDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @RequestBody @Validated(Update.class) BrukerDTO body)
      throws EInnsynException {
    var responseBody = service.update(brukerId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/bruker/{brukerId}")
  public ResponseEntity<BrukerDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId)
      throws EInnsynException {
    var responseBody = service.delete(brukerId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{brukerId}/innsynskrav")
  public ResponseEntity<ResultList<InnsynskravDTO>> getInnsynskravList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid InnsynskravListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getInnsynskravList(brukerId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{brukerId}/innsynskrav")
  public ResponseEntity<InnsynskravDTO> addInnsynskrav(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @RequestBody @Validated(Insert.class) InnsynskravDTO body)
      throws EInnsynException {
    var responseBody = service.addInnsynskrav(brukerId, body);
    var location = URI.create("/innsynskrav/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/bruker/{brukerId}/lagretSoek")
  public ResponseEntity<ResultList<LagretSoekDTO>> getLagretSoekList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid LagretSoekListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getLagretSoekList(brukerId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{brukerId}/lagretSoek")
  public ResponseEntity<LagretSoekDTO> addLagretSoek(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @RequestBody @Validated(Insert.class) LagretSoekDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSoek(brukerId, body);
    var location = URI.create("/lagretsoek/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/bruker/{brukerId}/lagretSak")
  public ResponseEntity<ResultList<LagretSakDTO>> getLagretSakList(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid LagretSakListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getLagretSakList(brukerId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{brukerId}/lagretSak")
  public ResponseEntity<LagretSakDTO> addLagretSak(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @RequestBody @Validated(Insert.class) LagretSakDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSak(brukerId, body);
    var location = URI.create("/lagretsak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @PutMapping("/bruker/{brukerId}/activate/{secret}")
  public ResponseEntity<BrukerDTO> activate(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.activate(brukerId, secret);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{brukerId}/updatePassword")
  public ResponseEntity<BrukerDTO> updatePassword(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @RequestBody @Validated(Update.class) PutBrukerPasswordDTO body)
      throws EInnsynException {
    var responseBody = service.updatePassword(brukerId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{brukerId}/updatePassword/{secret}")
  public ResponseEntity<BrukerDTO> updatePasswordWithSecret(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId,
      @Valid @PathVariable @NotNull String secret,
      @RequestBody @Validated(Update.class) PutBrukerPasswordWithSecretDTO body)
      throws EInnsynException {
    var responseBody = service.updatePasswordWithSecret(brukerId, secret, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/bruker/{brukerId}/requestPasswordReset")
  public ResponseEntity<BrukerDTO> requestPasswordReset(
      @Valid @PathVariable @NotNull @ExistingObject(service = BrukerService.class) String brukerId)
      throws EInnsynException {
    var responseBody = service.requestPasswordReset(brukerId);
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
