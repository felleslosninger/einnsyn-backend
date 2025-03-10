// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.apikey.models.ApiKeyDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiKeyController {
  private final ApiKeyService service;

  public ApiKeyController(ApiKeyService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/apiKey")
  public ResponseEntity<PaginatedList<ApiKeyDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/apiKey/{id}")
  public ResponseEntity<ApiKeyDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ApiKeyService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/apiKey/{id}")
  public ResponseEntity<ApiKeyDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ApiKeyService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/apiKey/{id}")
  public ResponseEntity<ApiKeyDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ApiKeyService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = ApiKeyService.class)
          @NotNull
          ApiKeyDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }
}
