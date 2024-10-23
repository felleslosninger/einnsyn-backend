// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.vedtak;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseListQueryDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
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
public class VedtakController {

  private final VedtakService service;

  public VedtakController(VedtakService service) {
    this.service = service;
  }

  @GetMapping("/vedtak")
  public ResponseEntity<ResultList<VedtakDTO>> list(@Valid BaseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(vedtakId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PutMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId,
      @RequestBody @Validated(Update.class) VedtakDTO body)
      throws EInnsynException {
    var responseBody = service.update(vedtakId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/vedtak/{vedtakId}")
  public ResponseEntity<VedtakDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId)
      throws EInnsynException {
    var responseBody = service.delete(vedtakId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/vedtak/{vedtakId}/vedtaksdokument")
  public ResponseEntity<ResultList<DokumentbeskrivelseDTO>> getVedtaksdokumentList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId,
      @Valid DokumentbeskrivelseListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getVedtaksdokumentList(vedtakId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/vedtak/{vedtakId}/vedtaksdokument")
  public ResponseEntity<DokumentbeskrivelseDTO> addVedtaksdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId,
      @RequestBody @Validated(Insert.class) DokumentbeskrivelseDTO body)
      throws EInnsynException {
    var responseBody = service.addVedtaksdokument(vedtakId, body);
    var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @DeleteMapping("/vedtak/{vedtakId}/vedtaksdokument/{vedtaksdokumentId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteVedtaksdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          String vedtakId,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String vedtaksdokumentId)
      throws EInnsynException {
    var responseBody = service.deleteVedtaksdokument(vedtakId, vedtaksdokumentId);
    return ResponseEntity.ok().body(responseBody);
  }
}
