// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
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
public class InnsynskravDelController {

  private final InnsynskravDelService service;

  public InnsynskravDelController(InnsynskravDelService service) {
    this.service = service;
  }

  @GetMapping("/innsynskravDel/{innsynskravDelId}")
  public ResponseEntity<InnsynskravDelDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravDelService.class)
          String innsynskravDelId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(innsynskravDelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/innsynskravDel/{innsynskravDelId}")
  public ResponseEntity<InnsynskravDelDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravDelService.class)
          String innsynskravDelId,
      @RequestBody @Validated(Update.class) InnsynskravDelDTO body)
      throws EInnsynException {
    var responseBody = service.update(innsynskravDelId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/innsynskravDel/{innsynskravDelId}")
  public ResponseEntity<InnsynskravDelDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravDelService.class)
          String innsynskravDelId)
      throws EInnsynException {
    var responseBody = service.delete(innsynskravDelId);
    return ResponseEntity.ok().body(responseBody);
  }
}
