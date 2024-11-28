// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.votering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.backend.entities.base.models.BaseListQueryDTO;
import no.einnsyn.backend.entities.votering.models.VoteringDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VoteringController {

  private final VoteringService service;

  public VoteringController(VoteringService service) {
    this.service = service;
  }

  @GetMapping("/votering")
  public ResponseEntity<ResultList<VoteringDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VoteringService.class, mustExist = true)
          String voteringId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(voteringId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VoteringService.class, mustExist = true)
          String voteringId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = VoteringService.class)
          VoteringDTO body)
      throws EInnsynException {
    var responseBody = service.update(voteringId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VoteringService.class, mustExist = true)
          String voteringId)
      throws EInnsynException {
    var responseBody = service.delete(voteringId);
    return ResponseEntity.ok().body(responseBody);
  }
}
