// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.arkivdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelListQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
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

  @PostMapping("/arkivdel")
  public ResponseEntity<ArkivdelDTO> add(@RequestBody @Validated(Insert.class) ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/arkivdel/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Update.class) ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> getKlasseList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlasseList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Insert.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(id, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/klassifikasjonssystem")
  public ResponseEntity<ResultList<KlassifikasjonssystemDTO>> getKlassifikasjonssystemList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid KlassifikasjonssystemListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlassifikasjonssystemList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/klassifikasjonssystem")
  public ResponseEntity<KlassifikasjonssystemDTO> addKlassifikasjonssystem(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Insert.class) KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.addKlassifikasjonssystem(id, body);
    var location = URI.create("/klassifikasjonssystem/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> getSaksmappeList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid SaksmappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getSaksmappeList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Insert.class) SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(id, body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> getMoetemappeList(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @Valid MoetemappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetemappeList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = ArkivdelService.class) String id,
      @RequestBody @Validated(Insert.class) MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(id, body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
