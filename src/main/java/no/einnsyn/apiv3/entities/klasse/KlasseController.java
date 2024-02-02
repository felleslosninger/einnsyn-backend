// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klasse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
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
public class KlasseController {

  private final KlasseService service;

  public KlasseController(KlasseService service) {
    this.service = service;
  }

  @GetMapping("/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> list(@Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klasse")
  public ResponseEntity<KlasseDTO> add(@RequestBody @Validated(Insert.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String id,
      @RequestBody @Validated(Update.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/klasse/{id}")
  public ResponseEntity<KlasseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
