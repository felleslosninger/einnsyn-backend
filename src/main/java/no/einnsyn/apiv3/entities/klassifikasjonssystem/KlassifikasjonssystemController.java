// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.KlasseService;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemListQueryDTO;
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
public class KlassifikasjonssystemController {

  private final KlassifikasjonssystemService service;

  public KlassifikasjonssystemController(KlassifikasjonssystemService service) {
    this.service = service;
  }

  @GetMapping("/klassifikasjonssystem")
  public ResponseEntity<ResultList<KlassifikasjonssystemDTO>> list(
      @Valid KlassifikasjonssystemListQueryDTO query) throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klassifikasjonssystem/{klassifikasjonssystemId}")
  public ResponseEntity<KlassifikasjonssystemDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String klassifikasjonssystemId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(klassifikasjonssystemId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/klassifikasjonssystem/{klassifikasjonssystemId}")
  public ResponseEntity<KlassifikasjonssystemDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String klassifikasjonssystemId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = KlassifikasjonssystemService.class)
          KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.update(klassifikasjonssystemId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/klassifikasjonssystem/{klassifikasjonssystemId}")
  public ResponseEntity<KlassifikasjonssystemDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String klassifikasjonssystemId)
      throws EInnsynException {
    var responseBody = service.delete(klassifikasjonssystemId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klassifikasjonssystem/{klassifikasjonssystemId}/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> getKlasseList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String klassifikasjonssystemId,
      @Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlasseList(klassifikasjonssystemId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klassifikasjonssystem/{klassifikasjonssystemId}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustExist = true)
          String klassifikasjonssystemId,
      @RequestBody @Validated(Insert.class) @ExpandableObject(service = KlasseService.class)
          KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(klassifikasjonssystemId, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
