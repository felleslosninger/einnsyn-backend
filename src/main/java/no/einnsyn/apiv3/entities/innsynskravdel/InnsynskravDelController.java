// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskravdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InnsynskravDelController {

  private final InnsynskravDelService service;

  public InnsynskravDelController(InnsynskravDelService service) {
    this.service = service;
  }

  @GetMapping("/innsynskravDel")
  public ResponseEntity<ResultList<InnsynskravDelDTO>> list(@Valid InnsynskravDelListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
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
}
