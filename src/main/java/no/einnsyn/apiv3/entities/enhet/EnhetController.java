package no.einnsyn.apiv3.entities.enhet;

import java.util.Arrays;
import org.springframework.http.HttpHeaders;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

@RestController
public class EnhetController {

  private final EnhetService enhetService;
  private final EnhetRepository enhetRepository;

  EnhetController(EnhetService enhetService, EnhetRepository enhetRepository) {
    this.enhetService = enhetService;
    this.enhetRepository = enhetRepository;
  }


  @GetMapping("/enhet")
  public ResponseEntity<ResponseList<EnhetJSON>> getEnhetList(
      @Valid GetListRequestParameters params) {
    ResponseList<EnhetJSON> response = enhetService.list(params);
    return ResponseEntity.ok(response);
  }


  @PostMapping("/enhet")
  public ResponseEntity<EnhetJSON> createEnhet(
      @Validated(Insert.class) @NewObject @RequestBody EnhetJSON enhetJSON,
      HttpServletRequest request) {
    EnhetJSON createdEnhet = enhetService.update(enhetJSON);

    // TODO: Add location header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<>(createdEnhet, headers, HttpStatus.CREATED);
  }


  @GetMapping("/enhet/{id}")
  public ResponseEntity<EnhetJSON> getEnhet(
      @Valid @ExistingObject(type = Enhet.class) @PathVariable String id,
      @Valid GetListRequestParameters params) {
    Enhet enhet = enhetRepository.findById(id);
    EnhetJSON enhetJSON = enhetService.toJSON(enhet);
    return ResponseEntity.ok(enhetJSON);
  }


  @PutMapping("/enhet/{id}")
  public ResponseEntity<EnhetJSON> updateEnhet(
      @Valid @ExistingObject(type = Enhet.class) @PathVariable String id,
      @Validated(Update.class) @NewObject @RequestBody EnhetJSON enhetJSON) {
    EnhetJSON updatedEnhet = enhetService.update(id, enhetJSON);
    return ResponseEntity.ok(updatedEnhet);
  }


  @DeleteMapping("/enhet/{id}")
  public ResponseEntity<EnhetJSON> deleteEnhet(
      @Valid @ExistingObject(type = Enhet.class) @PathVariable String id) {
    EnhetJSON deletedEnhetJSON = enhetService.delete(id);
    return ResponseEntity.ok(deletedEnhetJSON);
  }


  @PostMapping("/enhet/{id}/underenhet")
  public ResponseEntity<EnhetJSON> addUnderenhet(
      @Valid @ExistingObject(type = Enhet.class) @PathVariable String id,
      @Validated(Insert.class) @RequestBody EnhetJSON underenhetJSON, HttpServletRequest request) {

    // Create new underenhet (or get from DB if it exists)
    EnhetJSON createdUnderenhetJSON = enhetService.update(underenhetJSON);

    // Relate to parent Enhet
    EnhetJSON enhetUpdateJSON = new EnhetJSON();
    enhetUpdateJSON
        .setUnderenhet(Arrays.asList(new ExpandableField<EnhetJSON>(createdUnderenhetJSON)));
    enhetService.update(id, enhetUpdateJSON);

    // Set status to "created" if we're adding a new object, "ok" if it already exists
    HttpStatus status = underenhetJSON.getId() == null ? HttpStatus.CREATED : HttpStatus.OK;

    // TODO: Add location header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<>(createdUnderenhetJSON, headers, status);
  }
}
