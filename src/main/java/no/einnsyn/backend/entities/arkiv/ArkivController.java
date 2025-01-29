// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.arkiv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkiv.models.ListByArkivParameters;
import no.einnsyn.backend.entities.arkivdel.ArkivdelService;
import no.einnsyn.backend.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.backend.entities.moetemappe.MoetemappeService;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.saksmappe.SaksmappeService;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.error.exceptions.EInnsynException;
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
public class ArkivController {
  private final ArkivService service;

  public ArkivController(ArkivService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/arkiv")
  public ResponseEntity<ListResponseBody<ArkivDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv")
  public ResponseEntity<ArkivDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = ArkivService.class, mustNotExist = true)
          @NotNull
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/arkiv/{id}")
  public ResponseEntity<ArkivDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @RequestBody @Validated(Update.class) @ExpandableObject(service = ArkivService.class) @NotNull
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/arkiv/{id}/arkiv")
  public ResponseEntity<ListResponseBody<ArkivDTO>> listArkiv(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @Valid ListByArkivParameters query)
      throws EInnsynException {
    var responseBody = service.listArkiv(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/arkiv")
  public ResponseEntity<ArkivDTO> addArkiv(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = ArkivService.class, mustNotExist = true)
          @NotNull
          ArkivDTO body)
      throws EInnsynException {
    var responseBody = service.addArkiv(id, body);
    var location = URI.create("/arkiv/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{id}/arkivdel")
  public ResponseEntity<ListResponseBody<ArkivdelDTO>> listArkivdel(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @Valid ListByArkivParameters query)
      throws EInnsynException {
    var responseBody = service.listArkivdel(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/arkivdel")
  public ResponseEntity<ArkivdelDTO> addArkivdel(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = ArkivdelService.class, mustNotExist = true)
          @NotNull
          ArkivdelDTO body)
      throws EInnsynException {
    var responseBody = service.addArkivdel(id, body);
    var location = URI.create("/arkivdel/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{id}/moetemappe")
  public ResponseEntity<ListResponseBody<MoetemappeDTO>> listMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @Valid ListByArkivParameters query)
      throws EInnsynException {
    var responseBody = service.listMoetemappe(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/moetemappe")
  public ResponseEntity<MoetemappeDTO> addMoetemappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = MoetemappeService.class, mustNotExist = true)
          @NotNull
          MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetemappe(id, body);
    var location = URI.create("/moetemappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/arkiv/{id}/saksmappe")
  public ResponseEntity<ListResponseBody<SaksmappeDTO>> listSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @Valid ListByArkivParameters query)
      throws EInnsynException {
    var responseBody = service.listSaksmappe(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/arkiv/{id}/saksmappe")
  public ResponseEntity<SaksmappeDTO> addSaksmappe(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = ArkivService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = SaksmappeService.class, mustNotExist = true)
          @NotNull
          SaksmappeDTO body)
      throws EInnsynException {
    var responseBody = service.addSaksmappe(id, body);
    var location = URI.create("/saksmappe/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
