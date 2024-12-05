// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.innsynskravbestilling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.resultlist.ResultList;
import no.einnsyn.backend.entities.base.models.BaseGetQueryDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravListQueryDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingListQueryDTO;
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
public class InnsynskravBestillingController {

  private final InnsynskravBestillingService service;

  public InnsynskravBestillingController(InnsynskravBestillingService service) {
    this.service = service;
  }

  @GetMapping("/innsynskravBestilling")
  public ResponseEntity<ResultList<InnsynskravBestillingDTO>> list(
      @Valid InnsynskravBestillingListQueryDTO query) throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/innsynskravBestilling")
  public ResponseEntity<InnsynskravBestillingDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = InnsynskravBestillingService.class)
          InnsynskravBestillingDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/innsynskravBestilling/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  @GetMapping("/innsynskravBestilling/{innsynskravBestillingId}")
  public ResponseEntity<InnsynskravBestillingDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String innsynskravBestillingId,
      @Valid BaseGetQueryDTO query)
      throws EInnsynException {
    var responseBody = service.get(innsynskravBestillingId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/innsynskravBestilling/{innsynskravBestillingId}")
  public ResponseEntity<InnsynskravBestillingDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String innsynskravBestillingId,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = InnsynskravBestillingService.class)
          InnsynskravBestillingDTO body)
      throws EInnsynException {
    var responseBody = service.update(innsynskravBestillingId, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @DeleteMapping("/innsynskravBestilling/{innsynskravBestillingId}")
  public ResponseEntity<InnsynskravBestillingDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String innsynskravBestillingId)
      throws EInnsynException {
    var responseBody = service.delete(innsynskravBestillingId);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/innsynskravBestilling/{innsynskravBestillingId}/innsynskrav")
  public ResponseEntity<ResultList<InnsynskravDTO>> getInnsynskravList(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String innsynskravBestillingId,
      @Valid InnsynskravListQueryDTO query)
      throws EInnsynException {
    var responseBody = service.getInnsynskravList(innsynskravBestillingId, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/innsynskravBestilling/{innsynskravBestillingId}/verify/{secret}")
  public ResponseEntity<InnsynskravBestillingDTO> verifyInnsynskravBestilling(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String innsynskravBestillingId,
      @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.verifyInnsynskravBestilling(innsynskravBestillingId, secret);
    return ResponseEntity.ok().body(responseBody);
  }
}
