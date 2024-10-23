// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.behandlingsprotokoll;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.behandlingsprotokoll.models.BehandlingsprotokollDTO;
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
public class BehandlingsprotokollController {

  private final BehandlingsprotokollService service;

  public BehandlingsprotokollController(BehandlingsprotokollService service) {
    this.service = service;
  }

  @GetMapping("/behandlingsprotokoll")
  public ResponseEntity<ResultList<BehandlingsprotokollDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/behandlingsprotokoll/{behandlingsprotokollId}")
  public ResponseEntity<BehandlingsprotokollDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String behandlingsprotokollId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(behandlingsprotokollId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/behandlingsprotokoll/{behandlingsprotokollId}")
  public ResponseEntity<BehandlingsprotokollDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String behandlingsprotokollId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = BehandlingsprotokollService.class)
          BehandlingsprotokollDTO body)
      throws EInnsynException {
    var responseBody = service.update(behandlingsprotokollId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/behandlingsprotokoll/{behandlingsprotokollId}")
  public ResponseEntity<BehandlingsprotokollDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = BehandlingsprotokollService.class, mustExist = true)
          String behandlingsprotokollId)
      throws EInnsynException {
    var responseBody = service.delete(behandlingsprotokollId);
    return ResponseEntity.ok().body(responseBody);
  }
}
