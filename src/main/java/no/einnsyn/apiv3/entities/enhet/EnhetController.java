// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.enhet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetListQueryDTO;
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

@SuppressWarnings("java:S1130")
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

  @PostMapping("/enhet")
  public ResponseEntity<EnhetDTO> add(@RequestBody @Validated(Insert.class) EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/enhet/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @RequestBody @Validated(Update.class) EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/enhet/{id}")
  public ResponseEntity<EnhetDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/enhet/{id}/underenhet")
  public ResponseEntity<ResultList<EnhetDTO>> getUnderenhetList(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid EnhetListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getUnderenhetList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/enhet/{id}/underenhet")
  public ResponseEntity<EnhetDTO> addUnderenhet(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @RequestBody @Validated(Insert.class) EnhetDTO body)
      throws EInnsynException {
    var responseBody = service.addUnderenhet(id, body);
    var location = URI.create("/enhet/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/enhet/{id}/underenhet/{subId}")
  public ResponseEntity<EnhetDTO> deleteUnderenhet(
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = EnhetService.class) String subId)
      throws EInnsynException {
    var responseBody = service.deleteUnderenhet(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }
}
