// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
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

@SuppressWarnings("java:S1130")
@RestController
public class MoetesaksbeskrivelseController {

  private final MoetesaksbeskrivelseService service;

  public MoetesaksbeskrivelseController(MoetesaksbeskrivelseService service) {
    this.service = service;
  }

  @GetMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id,
      @RequestBody @Validated(Update.class) MoetesaksbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetesaksbeskrivelse/{id}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesaksbeskrivelseService.class)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }
}
