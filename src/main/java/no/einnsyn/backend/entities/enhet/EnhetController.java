// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.enhet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.ApiKeyService;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.entities.enhet.models.ListByEnhetParameters;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
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
public class EnhetController {
  private final EnhetService service;

  public EnhetController(EnhetService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/enhet")
  public ResponseEntity<PaginatedList<EnhetDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet")
  public ResponseEntity<EnhetDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = EnhetService.class, mustNotExist = true)
          @NotNull
          EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/enhet/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = EnhetService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = EnhetService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = EnhetService.class, mustExist = true)
          String id,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = EnhetService.class) @NotNull
          EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{id}/apiKey")
  public ResponseEntity<PaginatedList<ApiKeyDTO>> listApiKey(
      @Valid @PathVariable @NotNull String id, @Valid ListByEnhetParameters query)
      throws EInnsynException {
    var responseBody = service.listApiKey(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet/{id}/apiKey")
  public ResponseEntity<ApiKeyDTO> addApiKey(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = EnhetService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = ApiKeyService.class, mustNotExist = true)
          @NotNull
          ApiKeyDTO body)
      throws EInnsynException {
    var responseBody = service.addApiKey(id, body);
    var location = URI.create("/apikey/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/enhet/{id}/arkiv")
  public ResponseEntity<PaginatedList<ArkivDTO>> listArkiv(
      @Valid @PathVariable @NotNull String id, @Valid ListByEnhetParameters query)
      throws EInnsynException {
    var responseBody = service.listArkiv(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{id}/innsynskrav")
  public ResponseEntity<PaginatedList<InnsynskravDTO>> listInnsynskrav(
      @Valid @PathVariable @NotNull String id, @Valid ListByEnhetParameters query)
      throws EInnsynException {
    var responseBody = service.listInnsynskrav(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{id}/underenhet")
  public ResponseEntity<PaginatedList<EnhetDTO>> listUnderenhet(
      @Valid @PathVariable @NotNull String id, @Valid ListByEnhetParameters query)
      throws EInnsynException {
    var responseBody = service.listUnderenhet(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet/{id}/underenhet")
  public ResponseEntity<EnhetDTO> addUnderenhet(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = EnhetService.class, mustExist = true)
          String id,
      @RequestBody @Valid @NotNull ExpandableField<EnhetDTO> body)
      throws EInnsynException {
    var responseBody = service.addUnderenhet(id, body);
    if (body.getId() == null) {
      var location = URI.create("/enhet/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }
}
