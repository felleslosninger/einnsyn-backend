// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.vedtak;

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
import no.einnsyn.backend.entities.vedtak.models.ListByVedtakParameters;
import no.einnsyn.backend.entities.vedtak.models.VedtakDTO;
import no.einnsyn.backend.entities.votering.VoteringService;
import no.einnsyn.backend.entities.votering.models.VoteringDTO;
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
public class VedtakController {
  private final VedtakService service;

  public VedtakController(VedtakService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/vedtak")
  public ResponseEntity<PaginatedList<VedtakDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id)
      throws EInnsynException {
    var responseBody = service.delete(id.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/vedtak/{id}")
  public ResponseEntity<VedtakDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = VedtakService.class)
          @NotNull
          VedtakDTO body)
      throws EInnsynException {
    var responseBody = service.update(id.getId(), body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/vedtak/{id}/vedtaksdokument")
  public ResponseEntity<PaginatedList<DokumentbeskrivelseDTO>> listVedtaksdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @Valid ListByVedtakParameters query)
      throws EInnsynException {
    var responseBody = service.listVedtaksdokument(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/vedtak/{id}/vedtaksdokument")
  public ResponseEntity<DokumentbeskrivelseDTO> addVedtaksdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @RequestBody @Valid @NotNull ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    var responseBody = service.addVedtaksdokument(id.getId(), body);
    if (body.getId() == null) {
      var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }

  @DeleteMapping("/vedtak/{id}/vedtaksdokument/{vedtaksdokumentId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteVedtaksdokument(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          ExpandableField<DokumentbeskrivelseDTO> vedtaksdokumentId)
      throws EInnsynException {
    var responseBody = service.deleteVedtaksdokument(id.getId(), vedtaksdokumentId.getId());
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/vedtak/{id}/votering")
  public ResponseEntity<PaginatedList<VoteringDTO>> listVotering(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @Valid ListByVedtakParameters query)
      throws EInnsynException {
    var responseBody = service.listVotering(id.getId(), query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/vedtak/{id}/votering")
  public ResponseEntity<VoteringDTO> addVotering(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = VedtakService.class, mustExist = true)
          ExpandableField<VedtakDTO> id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = VoteringService.class, mustNotExist = true)
          @NotNull
          VoteringDTO body)
      throws EInnsynException {
    var responseBody = service.addVotering(id.getId(), body);
    var location = URI.create("/votering/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
