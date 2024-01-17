// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetemappe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakListQueryDTO;
import no.einnsyn.apiv3.entities.moetesaksbeskrivelse.models.MoetesaksbeskrivelseDTO;
import no.einnsyn.apiv3.entities.registrering.models.RegistreringDTO;
import no.einnsyn.apiv3.entities.utredning.models.UtredningDTO;
import no.einnsyn.apiv3.entities.vedtak.models.VedtakDTO;
import no.einnsyn.apiv3.validation.existingobject.ExistingObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MoetemappeController {

  private final MoetemappeService service;

  public MoetemappeController(MoetemappeService service) {
    this.service = service;
  }

  @GetMapping("/moetemappe")
  public ResponseEntity<ResultList<MoetemappeDTO>> list(
    @Valid BaseListQueryDTO query
  ) {
    try {
      var responseBody = service.list(query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.list", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe")
  public ResponseEntity<MoetemappeDTO> add(
    @RequestBody @Validated(Insert.class) MoetemappeDTO body
  ) {
    try {
      var responseBody = service.add(body);
      var location = URI.create("/moetemappe/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.add", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> get(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid BaseGetQueryDTO query
  ) {
    try {
      var responseBody = service.get(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.get", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PutMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> update(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @RequestBody @Validated(Update.class) MoetemappeDTO body
  ) {
    try {
      var responseBody = service.update(id, body);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.update", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}")
  public ResponseEntity<MoetemappeDTO> delete(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id
  ) {
    try {
      var responseBody = service.delete(id);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.delete", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<ResultList<MoetedokumentDTO>> getMoetedokumentList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid MoetedokumentListQueryDTO query
  ) {
    try {
      var responseBody = service.getMoetedokumentList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.getMoetedokumentList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe/{id}/moetedokument")
  public ResponseEntity<MoetedokumentDTO> addMoetedokument(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @RequestBody @Validated(Insert.class) MoetedokumentDTO body
  ) {
    try {
      var responseBody = service.addMoetedokument(id, body);
      var location = URI.create("/moetedokument/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.addMoetedokument", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}/moetedokument/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetedokumentFromMoetemappe(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetedokumentService.class
    ) String subId
  ) {
    try {
      var responseBody = service.removeMoetedokumentFromMoetemappe(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error(
        "Error executing MoetemappeService.removeMoetedokumentFromMoetemappe",
        e
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @GetMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<ResultList<MoetesakDTO>> getMoetesakList(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid MoetesakListQueryDTO query
  ) {
    try {
      var responseBody = service.getMoetesakList(id, query);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.getMoetesakList", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @PostMapping("/moetemappe/{id}/moetesak")
  public ResponseEntity<MoetesakDTO> addMoetesak(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @RequestBody @Validated(Insert.class) MoetesakDTO body
  ) {
    try {
      var responseBody = service.addMoetesak(id, body);
      var location = URI.create("/moetesak/" + responseBody.getId());
      return ResponseEntity.created(location).body(responseBody);
    } catch (Exception e) {
      log.error("Error executing MoetemappeService.addMoetesak", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }

  @DeleteMapping("/moetemappe/{id}/moetesak/{subId}")
  public ResponseEntity<MoetemappeDTO> removeMoetesakFromMoetemappe(
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetemappeService.class
    ) String id,
    @Valid @PathVariable @NotNull @ExistingObject(
      service = MoetesakService.class
    ) String subId
  ) {
    try {
      var responseBody = service.removeMoetesakFromMoetemappe(id, subId);
      return ResponseEntity.ok().body(responseBody);
    } catch (Exception e) {
      log.error(
        "Error executing MoetemappeService.removeMoetesakFromMoetemappe",
        e
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
  }
}
