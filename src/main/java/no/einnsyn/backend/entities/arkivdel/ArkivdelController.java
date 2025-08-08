// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.arkivdel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.arkivdel.models.ListByArkivdelParameters;
import no.einnsyn.backend.entities.klasse.KlasseService;
import no.einnsyn.backend.entities.klasse.models.KlasseDTO;
import no.einnsyn.backend.entities.klassifikasjonssystem.KlassifikasjonssystemService;
import no.einnsyn.backend.entities.klassifikasjonssystem.models.KlassifikasjonssystemDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
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

  /** List all objects. */
  @GetMapping("/arkivdel")
  public ResponseEntity<PaginatedList<ArkivdelDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/arkivdel/{id}")
  public ResponseEntity<ArkivdelDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = ArkivdelService.class)
          @NotNull
          ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/klasse")
  public ResponseEntity<PaginatedList<KlasseDTO>> listKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @Valid ListByArkivdelParameters query)
      throws EInnsynException {
    var responseBody = service.listKlasse(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/klasse")
  public ResponseEntity<KlasseDTO> addKlasse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = KlasseService.class, mustNotExist = true)
          @NotNull
          KlasseDTO body)
      throws EInnsynException {
    var responseBody = service.addKlasse(id.getId(), body);
    var location = URI.create("/klasse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/klassifikasjonssystem")
  public ResponseEntity<PaginatedList<KlassifikasjonssystemDTO>> listKlassifikasjonssystem(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @Valid ListByArkivdelParameters query)
      throws EInnsynException {
    var responseBody = service.listKlassifikasjonssystem(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/klassifikasjonssystem")
  public ResponseEntity<KlassifikasjonssystemDTO> addKlassifikasjonssystem(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = KlassifikasjonssystemService.class, mustNotExist = true)
          @NotNull
          KlassifikasjonssystemDTO body)
      throws EInnsynException {
    var responseBody = service.addKlassifikasjonssystem(id.getId(), body);
    var location = URI.create("/klassifikasjonssystem/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/moetemappe")
  public ResponseEntity<PaginatedList<MoetemappeDTO>> listMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @Valid ListByArkivdelParameters query)
      throws EInnsynException {
    var responseBody = service.listMoetemappe(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = MoetemappeService.class, mustNotExist = true)
          @NotNull
          MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(id.getId(), body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkivdel/{id}/saksmappe")
  public ResponseEntity<PaginatedList<SaksmappeDTO>> listSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @Valid ListByArkivdelParameters query)
      throws EInnsynException {
    var responseBody = service.listSaksmappe(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkivdel/{id}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivdelService.class, mustExist = true)
          ExpandableField<ArkivdelDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = SaksmappeService.class, mustNotExist = true)
          @NotNull
          SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(id.getId(), body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
