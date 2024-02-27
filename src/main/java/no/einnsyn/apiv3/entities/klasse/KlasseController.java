// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klasse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeListQueryDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
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
public class KlasseController {

  private final KlasseService service;

  public KlasseController(KlasseService service) {
    this.service = service;
  }

  @GetMapping("/klasse/{klasseId}")
  public ResponseEntity<KlasseDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(klasseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/klasse/{klasseId}")
  public ResponseEntity<KlasseDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @RequestBody @Validated(Update.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.update(klasseId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/klasse/{klasseId}")
  public ResponseEntity<KlasseDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId)
      throws EInnsynException {
    var responseBody = service.delete(klasseId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klasse/{klasseId}/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> getKlasseList(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlasseList(klasseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klasse/{klasseId}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @RequestBody @Validated(Insert.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(klasseId, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/klasse/{klasseId}/saksmappe")
  public ResponseEntity<ResultList<SaksmappeDTO>> getSaksmappeList(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @Valid SaksmappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getSaksmappeList(klasseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klasse/{klasseId}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @RequestBody @Validated(Insert.class) SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(klasseId, body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/klasse/{klasseId}/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> getMoetemappeList(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @Valid MoetemappeListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getMoetemappeList(klasseId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klasse/{klasseId}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlasseService.class) String klasseId,
      @RequestBody @Validated(Insert.class) MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(klasseId, body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
