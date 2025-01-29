// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.innsynskravbestilling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.ListResponseBody;
import no.einnsyn.backend.entities.innsynskrav.models.InnsynskravDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.InnsynskravBestillingDTO;
import no.einnsyn.backend.entities.innsynskravbestilling.models.ListByInnsynskravBestillingParameters;
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

  /** List all objects. */
  @GetMapping("/innsynskravBestilling")
  public ResponseEntity<ListResponseBody<InnsynskravBestillingDTO>> list(
      @Valid ListParameters query) throws EInnsynException {
    var responseBody = service.list(query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PostMapping("/innsynskravBestilling")
  public ResponseEntity<InnsynskravBestillingDTO> add(
      @RequestBody
          @Validated(Insert.class)
          @ExpandableObject(service = InnsynskravBestillingService.class, mustNotExist = true)
          @NotNull
          InnsynskravBestillingDTO body)
      throws EInnsynException {
    var responseBody = service.add(body);
    var location = URI.create("/innsynskravbestilling/" + responseBody.getId());
    return ResponseEntity.created(location).body(responseBody);
  }

  /** Delete an object. */
  @DeleteMapping("/innsynskravBestilling/{id}")
  public ResponseEntity<InnsynskravBestillingDTO> delete(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String id)
      throws EInnsynException {
    var responseBody = service.delete(id);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Get an object. */
  @GetMapping("/innsynskravBestilling/{id}")
  public ResponseEntity<InnsynskravBestillingDTO> get(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String id,
      @Valid GetParameters query)
      throws EInnsynException {
    var responseBody = service.get(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  /** Update an object. */
  @PatchMapping("/innsynskravBestilling/{id}")
  public ResponseEntity<InnsynskravBestillingDTO> update(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String id,
      @RequestBody
          @Validated(Update.class)
          @ExpandableObject(service = InnsynskravBestillingService.class)
          @NotNull
          InnsynskravBestillingDTO body)
      throws EInnsynException {
    var responseBody = service.update(id, body);
    return ResponseEntity.ok().body(responseBody);
  }

  @GetMapping("/innsynskravBestilling/{id}/innsynskrav")
  public ResponseEntity<ListResponseBody<InnsynskravDTO>> listInnsynskrav(
      @Valid
          @PathVariable
          @NotNull
          @ExpandableObject(service = InnsynskravBestillingService.class, mustExist = true)
          String id,
      @Valid ListByInnsynskravBestillingParameters query)
      throws EInnsynException {
    var responseBody = service.listInnsynskrav(id, query);
    return ResponseEntity.ok().body(responseBody);
  }

  @PatchMapping("/innsynskravBestilling/{id}/verify/{secret}")
  public ResponseEntity<InnsynskravBestillingDTO> verify(
      @Valid @PathVariable @NotNull String id, @Valid @PathVariable @NotNull String secret)
      throws EInnsynException {
    var responseBody = service.verify(id, secret);
    return ResponseEntity.ok().body(responseBody);
  }
}
