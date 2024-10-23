// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedokument;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
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
public class MoetedokumentController {

  private final MoetedokumentService service;

  public MoetedokumentController(MoetedokumentService service) {
    this.service = service;
  }

  @GetMapping("/moetedokument")
  public ResponseEntity<ResultList<MoetedokumentDTO>> list(@Valid MoetedokumentListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          String moetedokumentId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(moetedokumentId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          String moetedokumentId,
      @RequestBody @Validated(Update.class) MoetedokumentDTO body)
      throws EInnsynException {
    var responseBody = service.update(moetedokumentId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/moetedokument/{moetedokumentId}")
  public ResponseEntity<MoetedokumentDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          String moetedokumentId)
      throws EInnsynException {
    var responseBody = service.delete(moetedokumentId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetedokument/{moetedokumentId}/dokumentbeskrivelse")
  public ResponseEntity<ResultList<DokumentbeskrivelseDTO>> getDokumentbeskrivelseList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          String moetedokumentId,
      @Valid DokumentbeskrivelseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getDokumentbeskrivelseList(moetedokumentId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetedokument/{moetedokumentId}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetedokumentService.class, mustExist = true)
          String moetedokumentId,
      @RequestBody @Validated(Insert.class) ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    if (body.getId() != null) {
      var responseBody = service.addDokumentbeskrivelse(moetedokumentId, body);
      return ResponseEntity.ok().body(responseBody);
    }

    var responseBody = service.addDokumentbeskrivelse(moetedokumentId, body);
    var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
