// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedeltaker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
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
public class MoetedeltakerController {

  private final MoetedeltakerService service;

  public MoetedeltakerController(MoetedeltakerService service) {
    this.service = service;
  }

  @GetMapping("/moetedeltaker")
  public ResponseEntity<ResultList<MoetedeltakerDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetedeltaker/{moetedeltakerId}")
  public ResponseEntity<MoetedeltakerDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String moetedeltakerId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetedeltakerId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetedeltaker/{moetedeltakerId}")
  public ResponseEntity<MoetedeltakerDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String moetedeltakerId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = MoetedeltakerService.class)
          MoetedeltakerDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetedeltakerId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetedeltaker/{moetedeltakerId}")
  public ResponseEntity<MoetedeltakerDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedeltakerService.class, mustExist = true)
          String moetedeltakerId)
      throws EInnsynException {
    var responseBody = service.delete(moetedeltakerId);
    return ResponseEntity.ok().body(responseBody);
  }
}
