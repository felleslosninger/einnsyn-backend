// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.moetedokument.MoetedokumentService;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.MoetesakService;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
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

@SuppressWarnings("java:S1130")
@RestController
public class MoetemappeController {

  private final MoetemappeService service;

  public MoetemappeController(MoetemappeService service) {
    this.service = service;
  }

  @GetMapping("/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe")
  public ResponseEntity<MoetemappeDTO> add(@RequestBody @Validated(Insert.class) MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @RequestBody @Validated(Update.class) MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<ResultList<MoetedokumentDTO>> getMoetedokumentList(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @Valid MoetedokumentListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetedokumentList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<MoetedokumentDTO> addMoetedokument(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @RequestBody @Validated(Insert.class) MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetedokument(id, body);
    var location = URI.create("/moetedokument/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/moetemappe/{id}/moetedokument/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetedokumentFromMoetemappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.removeMoetedokumentFromMoetemappe(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<ResultList<MoetesakDTO>> getMoetesakList(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @Valid MoetesakListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetesakList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<MoetesakDTO> addMoetesak(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @RequestBody @Validated(Insert.class) MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetesak(id, body);
    var location = URI.create("/moetesak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/moetemappe/{id}/moetesak/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetesakFromMoetemappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class) String id,
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesakService.class) String subId)
      throws EInnsynException {
    var responseBody = service.removeMoetesakFromMoetemappe(id, subId);
    return ResponseEntity.ok().body(responseBody);
  }
}
