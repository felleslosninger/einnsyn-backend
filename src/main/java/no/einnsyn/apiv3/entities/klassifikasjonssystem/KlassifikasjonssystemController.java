// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klassifikasjonssystem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseListQueryDTO;
import no.einnsyn.apiv3.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
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
public class KlassifikasjonssystemController {

  private final KlassifikasjonssystemService service;

  public KlassifikasjonssystemController(KlassifikasjonssystemService service) {
    this.service = service;
  }

  @GetMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> get(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlassifikasjonssystemService.class)
          String id,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> update(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlassifikasjonssystemService.class)
          String id,
      @RequestBody @Validated(Update.class) KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/klassifikasjonssystem/{id}")
  public ResponseEntity<KlassifikasjonssystemDTO> delete(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlassifikasjonssystemService.class)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/klassifikasjonssystem/{id}/klasse")
  public ResponseEntity<ResultList<KlasseDTO>> getKlasseList(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlassifikasjonssystemService.class)
          String id,
      @Valid KlasseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getKlasseList(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/klassifikasjonssystem/{id}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid @PathVariable @NotNull @ExistingObject(service = KlassifikasjonssystemService.class)
          String id,
      @RequestBody @Validated(Insert.class) KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(id, body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
