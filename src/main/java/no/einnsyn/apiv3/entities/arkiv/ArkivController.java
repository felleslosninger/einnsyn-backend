// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkiv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivListQueryDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
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
public class ArkivController {

  private final ArkivService service;

  public ArkivController(ArkivService service) {
    this.service = service;
  }

  @GetMapping("/arkiv")
  public ResponseEntity<ResultList<ArkivDTO>> list(@Valid ArkivListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv")
  public ResponseEntity<ArkivDTO> add(@RequestBody @Validated(Insert.class) ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @RequestBody @Validated(Update.class) ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkiv/{id}/arkivdel")
  public ResponseEntity<ResultList<ArkivdelDTO>> getArkivdelList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @Valid ArkivdelListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getArkivdelList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/arkivdel")
  public ResponseEntity<ArkivdelDTO> addArkivdel(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @RequestBody @Validated(Insert.class) ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.addArkivdel(id, body);
    var location = URI.create("/arkivdel/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{id}/arkiv")
  public ResponseEntity<ResultList<ArkivDTO>> getArkivList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @Valid ArkivListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getArkivList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/arkiv")
  public ResponseEntity<ArkivDTO> addArkiv(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivService.class) String id,
      @RequestBody @Validated(Insert.class) ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.addArkiv(id, body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
