// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.innsynskrav;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelDTO;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = InnsynskravService.class)
          InnsynskravDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/innsynskrav/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
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

  @PatchMapping("/innsynskrav/{innsynskravId}")
  public ResponseEntity<InnsynskravDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravService.class, mustExist = true)
          String innsynskravId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = InnsynskravService.class)
          InnsynskravDTO body)
      throws EInnsynException {
    var responseBody = service.update(innsynskravId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/innsynskrav/{innsynskravId}")
  public ResponseEntity<InnsynskravDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravService.class, mustExist = true)
          String innsynskravId)
      throws EInnsynException {
    var responseBody = service.delete(innsynskravId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/innsynskrav/{innsynskravId}/innsynskravDel")
  public ResponseEntity<ResultList<InnsynskravDelDTO>> getInnsynskravDelList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravService.class, mustExist = true)
          String innsynskravId,
      @Valid InnsynskravDelListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getInnsynskravDelList(innsynskravId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/innsynskrav/{innsynskravId}/verify/{secret}")
  public ResponseEntity<InnsynskravDTO> verifyInnsynskrav(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravService.class, mustExist = true)
          String innsynskravId,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.verifyInnsynskrav(innsynskravId, secret);
    return ResponseEntity.ok().body(responseBody);
  }
}
