// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
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
public class EnhetController {

  private final EnhetService service;

  public EnhetController(EnhetService service) {
    this.service = service;
  }

  @GetMapping("/enhet")
  public ResponseEntity<ResultList<EnhetDTO>> list(@Valid EnhetListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{enhetId}")
  public ResponseEntity<EnhetDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(enhetId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/enhet/{enhetId}")
  public ResponseEntity<EnhetDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @RequestBody @Validated(Update.class) EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.update(enhetId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/enhet/{enhetId}")
  public ResponseEntity<EnhetDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId)
      throws EInnsynException {
    var responseBody = service.delete(enhetId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{enhetId}/underenhet")
  public ResponseEntity<ResultList<EnhetDTO>> getUnderenhetList(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @Valid EnhetListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getUnderenhetList(enhetId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet/{enhetId}/underenhet")
  public ResponseEntity<EnhetDTO> addUnderenhet(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @RequestBody @Validated(Insert.class) EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.addUnderenhet(enhetId, body);
    var location = URI.create("/enhet/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/enhet/{enhetId}/apiKey")
  public ResponseEntity<ResultList<ApiKeyDTO>> getApiKeyList(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @Valid ApiKeyListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getApiKeyList(enhetId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet/{enhetId}/apiKey")
  public ResponseEntity<ApiKeyDTO> addApiKey(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String enhetId,
      @RequestBody @Validated(Insert.class) ApiKeyDTO body)
      throws EInnsynException {
    var responseBody = service.addApiKey(enhetId, body);
    var location = URI.create("/apikey/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
