// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
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
public class InnsynskravController {

  private final InnsynskravService service;

  public InnsynskravController(InnsynskravService service) {
    this.service = service;
  }

  @GetMapping("/innsynskrav")
  public ResponseEntity<ResultList<InnsynskravDTO>> list(@Valid InnsynskravListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/innsynskrav")
  public ResponseEntity<InnsynskravDTO> add(
      @RequestBody @Validated(Insert.class) InnsynskravDTO body) throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/innsynskrav/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/innsynskrav/{id}")
  public ResponseEntity<InnsynskravDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/innsynskrav/{id}")
  public ResponseEntity<InnsynskravDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravService.class) String id,
      @RequestBody @Validated(Update.class) InnsynskravDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/innsynskrav/{id}")
  public ResponseEntity<InnsynskravDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/innsynskrav/{id}/verify/{secret}")
  public ResponseEntity<InnsynskravDTO> verifyInnsynskrav(
      @Valid @PathVariable @NotNull @ExistingObject(service = InnsynskravService.class) String id,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.verifyInnsynskrav(id, secret);
    return ResponseEntity.ok().body(responseBody);
  }
}
