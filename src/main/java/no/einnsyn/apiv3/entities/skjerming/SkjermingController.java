// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.skjerming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.skjerming.models.SkjermingDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
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
public class SkjermingController {

  private final SkjermingService service;

  public SkjermingController(SkjermingService service) {
    this.service = service;
  }

  @GetMapping("/skjerming")
  public ResponseEntity<ResultList<SkjermingDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/skjerming")
  public ResponseEntity<SkjermingDTO> add(@RequestBody @Validated(Insert.class) SkjermingDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/skjerming/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/skjerming/{skjermingId}")
  public ResponseEntity<SkjermingDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String skjermingId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(skjermingId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/skjerming/{skjermingId}")
  public ResponseEntity<SkjermingDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String skjermingId,
      @RequestBody @Validated(Update.class) SkjermingDTO body)
      throws EInnsynException {
    var responseBody = service.update(skjermingId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/skjerming/{skjermingId}")
  public ResponseEntity<SkjermingDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = SkjermingService.class, mustExist = true)
          String skjermingId)
      throws EInnsynException {
    var responseBody = service.delete(skjermingId);
    return ResponseEntity.ok().body(responseBody);
  }
}
