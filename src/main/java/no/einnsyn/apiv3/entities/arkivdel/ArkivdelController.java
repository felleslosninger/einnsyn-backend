// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkivdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.KlasseService;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemListQueryDTO;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArkivdelController {

  private final ArkivdelService service;

  public ArkivdelController(ArkivdelService service) {
    this.service = service;
  }

  @GetMapping("/arkivdel")
  public ResponseEntity<ResultList<ArkivdelDTO>> list(@Valid ArkivdelListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkivdel/{arkivdelId}")
  public ResponseEntity<ArkivdelDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(arkivdelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/arkivdel/{arkivdelId}")
  public ResponseEntity<ArkivdelDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = ArkivdelService.class)
          ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.update(arkivdelId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/arkivdel/{arkivdelId}")
  public ResponseEntity<ArkivdelDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId)
      throws EInnsynException {
    var responseBody = service.delete(arkivdelId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkivdel/{arkivdelId}/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> getKlasseList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlasseList(arkivdelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{arkivdelId}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = KlasseService.class)
          KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(arkivdelId, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{arkivdelId}/klassifikasjonssystem")
  public ResponseEntity<ResultList<KlassifikasjonssystemDTO>> getKlassifikasjonssystemList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @Valid KlassifikasjonssystemListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlassifikasjonssystemList(arkivdelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{arkivdelId}/klassifikasjonssystem")
  public ResponseEntity<KlassifikasjonssystemDTO> addKlassifikasjonssystem(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = KlassifikasjonssystemService.class)
          KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.addKlassifikasjonssystem(arkivdelId, body);
    var location = URI.create("/klassifikasjonssystem/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{arkivdelId}/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> getSaksmappeList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @Valid SaksmappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getSaksmappeList(arkivdelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{arkivdelId}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = SaksmappeService.class)
          SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(arkivdelId, body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{arkivdelId}/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> getMoetemappeList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @Valid MoetemappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetemappeList(arkivdelId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{arkivdelId}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          String arkivdelId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = MoetemappeService.class)
          MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(arkivdelId, body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
