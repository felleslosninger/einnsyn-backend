// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetemappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.moetedokument.MoetedokumentService;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetemappe.models.ListByMoetemappeParameters;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.MoetesakService;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
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
public class MoetemappeController {
  private final MoetemappeService service;

  public MoetemappeController(MoetemappeService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/moetemappe")
  public ResponseEntity<PaginatedList<MoetemappeDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = MoetemappeService.class)
          @NotNull
          MoetemappeDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<PaginatedList<MoetedokumentDTO>> listMoetedokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @Valid ListByMoetemappeParameters query)
      throws EInnsynException {
    var responseBody = service.listMoetedokument(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<MoetedokumentDTO> addMoetedokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = MoetedokumentService.class, mustNotExist = true)
          @NotNull
          MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetedokument(id, body);
    var location = URI.create("/moetedokument/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<PaginatedList<MoetesakDTO>> listMoetesak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @Valid ListByMoetemappeParameters query)
      throws EInnsynException {
    var responseBody = service.listMoetesak(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<MoetesakDTO> addMoetesak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetemappeService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = MoetesakService.class, mustNotExist = true)
          @NotNull
          MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.addMoetesak(id, body);
    var location = URI.create("/moetesak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
