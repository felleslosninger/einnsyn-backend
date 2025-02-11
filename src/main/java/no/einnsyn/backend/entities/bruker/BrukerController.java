// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.bruker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.bruker.models.BrukerDTO;
import no.einnsyn.backend.entities.bruker.models.ListByBrukerParameters;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.InnsynskravBestillingService;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.lagretsak.LagretSakService;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.entities.lagretsoek.LagretSoekService;
import no.einnsyn.backend.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrukerController {
  private final BrukerService service;

  public BrukerController(BrukerService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/bruker")
  public ResponseEntity<PaginatedList<BrukerDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker")
  public ResponseEntity<BrukerDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = BrukerService.class, mustNotExist = true)
          @NotNull
          BrukerDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/bruker/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/bruker/{id}")
  public ResponseEntity<BrukerDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = BrukerService.class)
          @NotNull
          BrukerDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/bruker/{id}/activate/{secret}")
  public ResponseEntity<BrukerDTO> activate(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.activate(id, secret);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{id}/innsynskrav")
  public ResponseEntity<PaginatedList<InnsynskravDTO>> listInnsynskrav(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid ListByBrukerParameters query)
      throws EInnsynException {
    var responseBody = service.listInnsynskrav(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/bruker/{id}/innsynskravBestilling")
  public ResponseEntity<PaginatedList<InnsynskravBestillingDTO>> listInnsynskravBestilling(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid ListByBrukerParameters query)
      throws EInnsynException {
    var responseBody = service.listInnsynskravBestilling(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/innsynskravBestilling")
  public ResponseEntity<InnsynskravBestillingDTO> addInnsynskravBestilling(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = InnsynskravBestillingService.class, mustNotExist = true)
          @NotNull
          InnsynskravBestillingDTO body)
      throws EInnsynException {
    var responseBody = service.addInnsynskravBestilling(id, body);
    var location = URI.create("/innsynskravbestilling/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<PaginatedList<LagretSakDTO>> listLagretSak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid ListByBrukerParameters query)
      throws EInnsynException {
    var responseBody = service.listLagretSak(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/lagretSak")
  public ResponseEntity<LagretSakDTO> addLagretSak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = LagretSakService.class, mustNotExist = true)
          @NotNull
          LagretSakDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSak(id, body);
    var location = URI.create("/lagretsak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<PaginatedList<LagretSoekDTO>> listLagretSoek(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid ListByBrukerParameters query)
      throws EInnsynException {
    var responseBody = service.listLagretSoek(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/bruker/{id}/lagretSoek")
  public ResponseEntity<LagretSoekDTO> addLagretSoek(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = LagretSoekService.class, mustNotExist = true)
          @NotNull
          LagretSoekDTO body)
      throws EInnsynException {
    var responseBody = service.addLagretSoek(id, body);
    var location = URI.create("/lagretsoek/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @PatchMapping("/bruker/{id}/requestPasswordReset")
  public ResponseEntity<BrukerDTO> requestPasswordReset(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.requestPasswordReset(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/bruker/{id}/updatePassword")
  public ResponseEntity<BrukerDTO> updatePassword(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @RequestBody @Valid @NotNull UpdatePasswordRequest body)
      throws EInnsynException {
    var responseBody = service.updatePassword(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/bruker/{id}/updatePassword/{secret}")
  public ResponseEntity<BrukerDTO> updatePasswordWithSecret(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BrukerService.class, mustExist = true)
          String id,
      @Valid @PathVariable @NotNull String secret,
      @RequestBody @Valid @NotNull UpdatePasswordWithSecretRequest body)
      throws EInnsynException {
    var responseBody = service.updatePasswordWithSecret(id, secret, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @Getter
  @Setter
  public class UpdatePasswordRequest {
    @NoSSN
    @Size(max = 500)
    @NotBlank(groups = {Insert.class})
    String oldPassword;

    @NoSSN
    @Size(max = 500)
    @NotBlank(groups = {Insert.class})
    String newPassword;
  }

  @Getter
  @Setter
  public class UpdatePasswordWithSecretRequest {
    @NoSSN
    @Size(max = 500)
    @NotBlank(groups = {Insert.class})
    String newPassword;
  }
}
