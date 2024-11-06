// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetesaksbeskrivelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MoetesaksbeskrivelseController {

  private final MoetesaksbeskrivelseService service;

  public MoetesaksbeskrivelseController(MoetesaksbeskrivelseService service) {
    this.service = service;
  }

  @GetMapping("/moetesaksbeskrivelse")
  public ResponseEntity<ResultList<MoetesaksbeskrivelseDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetesaksbeskrivelse/{moetesaksbeskrivelseId}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesaksbeskrivelseService.class, mustExist = true)
          String moetesaksbeskrivelseId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetesaksbeskrivelseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/moetesaksbeskrivelse/{moetesaksbeskrivelseId}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesaksbeskrivelseService.class, mustExist = true)
          String moetesaksbeskrivelseId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = MoetesaksbeskrivelseService.class)
          MoetesaksbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetesaksbeskrivelseId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetesaksbeskrivelse/{moetesaksbeskrivelseId}")
  public ResponseEntity<MoetesaksbeskrivelseDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesaksbeskrivelseService.class, mustExist = true)
          String moetesaksbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.delete(moetesaksbeskrivelseId);
    return ResponseEntity.ok().body(responseBody);
  }
}
