// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetesak;

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
import no.einnsyn.backend.entities.moetesak.models.GetByMoetesakParameters;
import no.einnsyn.backend.entities.moetesak.models.ListByMoetesakParameters;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.utredning.UtredningService;
import no.einnsyn.backend.entities.utredning.models.UtredningDTO;
import no.einnsyn.backend.entities.vedtak.VedtakService;
import no.einnsyn.backend.entities.vedtak.models.VedtakDTO;
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
public class MoetesakController {
  private final MoetesakService service;

  public MoetesakController(MoetesakService service) {
    this.service = service;
  }

  /** List all objects. */
  @GetMapping("/moetesak")
  public ResponseEntity<PaginatedList<MoetesakDTO>> list(@Valid ListParameters query)
      throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetesak")
  public ResponseEntity<MoetesakDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = MoetesakService.class, mustNotExist = true)
          @NotNull
          MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/moetesak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/moetesak/{id}")
  public ResponseEntity<MoetesakDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/moetesak/{id}")
  public ResponseEntity<MoetesakDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/moetesak/{id}")
  public ResponseEntity<MoetesakDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = MoetesakService.class)
          @NotNull
          MoetesakDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetesak/{id}/dokumentbeskrivelse")
  public ResponseEntity<PaginatedList<DokumentbeskrivelseDTO>> listDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @Valid ListByMoetesakParameters query)
      throws EInnsynException {
    var responseBody = service.listDokumentbeskrivelse(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetesak/{id}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseDTO> addDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @RequestBody @Valid @NotNull ExpandableField<DokumentbeskrivelseDTO> body)
      throws EInnsynException {
    var responseBody = service.addDokumentbeskrivelse(id, body);
    if (body.getId() == null) {
      var location = URI.create("/dokumentbeskrivelse/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } else {
      return ResponseEntity.ok().body(responseBody);
    }
  }

  @DeleteMapping("/moetesak/{id}/dokumentbeskrivelse/{dokumentbeskrivelseId}")
  public ResponseEntity<DokumentbeskrivelseDTO> deleteDokumentbeskrivelse(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = DokumentbeskrivelseService.class, mustExist = true)
          String dokumentbeskrivelseId)
      throws EInnsynException {
    var responseBody = service.deleteDokumentbeskrivelse(id, dokumentbeskrivelseId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/moetesak/{id}/utredning")
  public ResponseEntity<UtredningDTO> getUtredning(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @Valid GetByMoetesakParameters query)
      throws EInnsynException {
    var responseBody = service.getUtredning(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetesak/{id}/utredning")
  public ResponseEntity<UtredningDTO> addUtredning(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = UtredningService.class, mustNotExist = true)
          @NotNull
          UtredningDTO body)
      throws EInnsynException {
    var responseBody = service.addUtredning(id, body);
    var location = URI.create("/utredning/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/moetesak/{id}/vedtak")
  public ResponseEntity<VedtakDTO> getVedtak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @Valid GetByMoetesakParameters query)
      throws EInnsynException {
    var responseBody = service.getVedtak(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/moetesak/{id}/vedtak")
  public ResponseEntity<VedtakDTO> addVedtak(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = MoetesakService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = VedtakService.class, mustNotExist = true)
          @NotNull
          VedtakDTO body)
      throws EInnsynException {
    var responseBody = service.addVedtak(id, body);
    var location = URI.create("/vedtak/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }
}
