// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.votering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.votering.models.VoteringDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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
public class VoteringController {

  private final VoteringService service;

  public VoteringController(VoteringService service) {
    this.service = service;
  }

  @GetMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = VoteringService.class)
          String voteringId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(voteringId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = VoteringService.class)
          String voteringId,
      @RequestBody @Validated(Update.class) VoteringDTO body)
      throws EInnsynException {
    var responseBody = service.update(voteringId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/votering/{voteringId}")
  public ResponseEntity<VoteringDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = VoteringService.class)
          String voteringId)
      throws EInnsynException {
    var responseBody = service.delete(voteringId);
    return ResponseEntity.ok().body(responseBody);
  }
}
