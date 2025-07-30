// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetedokument;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.moetedokument.models.ListByMoetedokumentParameters;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
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
public class MoetedokumentController {
  private final MoetedokumentService service;

  public MoetedokumentController(MoetedokumentService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/moetedokument")
  public ResponseEntity<PaginatedList<MoetedokumentDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/moetedokument/{id}")
  public ResponseEntity<MoetedokumentDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = MoetedokumentService.class)
          @NotNull
          MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetedokument/{id}/dokumentbeskrivelse")
  public ResponseEntity<PaginatedList<DokumentbeskrivelseDTO>> listDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id,
      @Valid ListByMoetedokumentParameters query)
      throws EInnsynException {
    var responseBody = service.listDokumentbeskrivelse(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetedokument/{id}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id,
      @RequestBody @Valid @NotNull ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    var responseBody = service.addDokumentbeskrivelse(id.getId(), body);
    if (body.getId() == null) {
      var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }

  @DeleteMapping("/moetedokument/{id}/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          ExpandableField<MoetedokumentDTO> id,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.deleteDokumentbeskrivelse(id.getId(), dokumentbeskrivelseId.getId());
    return ResponseEntity.ok().body(responseBody);
  }
}
