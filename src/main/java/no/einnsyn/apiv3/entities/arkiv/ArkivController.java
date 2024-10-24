// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkiv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivListQueryDTO;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.MoetemappeService;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
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
public class ArkivController {

  private final ArkivService service;

  public ArkivController(ArkivService service) {
    this.service = service;
  }

  @GetMapping("/arkiv")
  public ResponseEntity<ResultList<ArkivDTO>> list(@Valid ArkivListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv")
  public ResponseEntity<ArkivDTO> add(
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = ArkivService.class)
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{arkivId}")
  public ResponseEntity<ArkivDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(arkivId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/arkiv/{arkivId}")
  public ResponseEntity<ArkivDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = ArkivService.class)
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.update(arkivId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/arkiv/{arkivId}")
  public ResponseEntity<ArkivDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId)
      throws EInnsynException {
    var responseBody = service.delete(arkivId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkiv/{arkivId}/arkivdel")
  public ResponseEntity<ResultList<ArkivdelDTO>> getArkivdelList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @Valid ArkivdelListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getArkivdelList(arkivId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{arkivId}/arkivdel")
  public ResponseEntity<ArkivdelDTO> addArkivdel(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = ArkivdelService.class)
          ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.addArkivdel(arkivId, body);
    var location = URI.create("/arkivdel/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{arkivId}/arkiv")
  public ResponseEntity<ResultList<ArkivDTO>> getArkivList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @Valid ArkivListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getArkivList(arkivId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{arkivId}/arkiv")
  public ResponseEntity<ArkivDTO> addArkiv(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = ArkivService.class)
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.addArkiv(arkivId, body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{arkivId}/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> getSaksmappeList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @Valid SaksmappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getSaksmappeList(arkivId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{arkivId}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = SaksmappeService.class)
          SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(arkivId, body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{arkivId}/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> getMoetemappeList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @Valid MoetemappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetemappeList(arkivId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{arkivId}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String arkivId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = MoetemappeService.class)
          MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(arkivId, body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
