// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/innsynskrav/{innsynskravId}")
  public ResponseEntity<InnsynskravDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravService.class, mustExist = true)
          String innsynskravId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(innsynskravId, query);
    return ResponseEntity.ok().body(responseBody);
  }
}
