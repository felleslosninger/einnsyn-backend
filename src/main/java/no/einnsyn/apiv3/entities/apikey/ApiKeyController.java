// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiKeyController {

  private final ApiKeyService service;

  public ApiKeyController(ApiKeyService service) {
    this.service = service;
  }

  @GetMapping("/apiKey/{apiKeyId}")
  public ResponseEntity<ApiKeyDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = ApiKeyService.class) String apiKeyId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(apiKeyId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/apiKey/{apiKeyId}")
  public ResponseEntity<ApiKeyDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = ApiKeyService.class) String apiKeyId,
      @RequestBody @Validated(Update.class) ApiKeyDTO body)
      throws EInnsynException {
    var responseBody = service.update(apiKeyId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/apiKey/{apiKeyId}")
  public ResponseEntity<ApiKeyDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = ApiKeyService.class) String apiKeyId)
      throws EInnsynException {
    var responseBody = service.delete(apiKeyId);
    return ResponseEntity.ok().body(responseBody);
  }
}
