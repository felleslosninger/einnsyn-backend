// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
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

@RestController
public class MoetemappeController {

  private final MoetemappeService service;

  public MoetemappeController(MoetemappeService service) {
    this.service = service;
  }

  @GetMapping("/moetemappe/{moetemappeId}")
  public ResponseEntity<MoetemappeDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetemappeId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetemappe/{moetemappeId}")
  public ResponseEntity<MoetemappeDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @RequestBody @Validated(Update.class) MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetemappeId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetemappe/{moetemappeId}")
  public ResponseEntity<MoetemappeDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId)
      throws EInnsynException {
    var responseBody = service.delete(moetemappeId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetemappe/{moetemappeId}/moetedokument")
  public ResponseEntity<ResultList<MoetedokumentDTO>> getMoetedokumentList(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @Valid MoetedokumentListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetedokumentList(moetemappeId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{moetemappeId}/moetedokument")
  public ResponseEntity<MoetedokumentDTO> addMoetedokument(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @RequestBody @Validated(Insert.class) MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetedokument(moetemappeId, body);
    var location = URI.create("/moetedokument/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/moetemappe/{moetemappeId}/moetedokument/{subId}")
  public ResponseEntity<MoetemappeDTO> deleteMoetedokument(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetedokumentService.class)
          String subId)
      throws EInnsynException {
    var responseBody = service.deleteMoetedokument(moetemappeId, subId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetemappe/{moetemappeId}/moetesak")
  public ResponseEntity<ResultList<MoetesakDTO>> getMoetesakList(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @Valid MoetesakListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetesakList(moetemappeId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{moetemappeId}/moetesak")
  public ResponseEntity<MoetesakDTO> addMoetesak(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @RequestBody @Validated(Insert.class) MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetesak(moetemappeId, body);
    var location = URI.create("/moetesak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/moetemappe/{moetemappeId}/moetesak/{subId}")
  public ResponseEntity<MoetemappeDTO> deleteMoetesak(
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetemappeService.class)
          String moetemappeId,
      @Valid @PathVariable @NotNull @ExistingObject(service = MoetesakService.class) String subId)
      throws EInnsynException {
    var responseBody = service.deleteMoetesak(moetemappeId, subId);
    return ResponseEntity.ok().body(responseBody);
  }
}
