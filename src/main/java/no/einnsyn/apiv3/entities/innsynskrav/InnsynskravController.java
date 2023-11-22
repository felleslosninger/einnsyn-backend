package no.einnsyn.apiv3.entities.innsynskrav;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.requests.GetSingleRequestParameters;

@RestController
public class InnsynskravController {

  private final InnsynskravService innsynskravService;
  private final InnsynskravRepository innsynskravRepository;

  InnsynskravController(InnsynskravService innsynskravService,
      InnsynskravRepository innsynskravRepository) {
    this.innsynskravService = innsynskravService;
    this.innsynskravRepository = innsynskravRepository;
  }


  @PostMapping("/innsynskrav")
  public ResponseEntity<InnsynskravJSON> createInnsynskrav(
      @Validated(Insert.class) @NewObject @RequestBody InnsynskravJSON innsynskravJSON,
      HttpServletRequest request) {
    InnsynskravJSON createdInnsynskravJSON = innsynskravService.update(innsynskravJSON);

    // TODO: Add location header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<>(createdInnsynskravJSON, headers, HttpStatus.CREATED);
  }


  @DeleteMapping("/innsynskrav/{id}")
  public ResponseEntity<InnsynskravJSON> deleteInnsynskrav(
      @Valid @ExistingObject(type = Innsynskrav.class) @PathVariable String id) {
    InnsynskravJSON json = innsynskravService.delete(id);
    return ResponseEntity.ok(json);
  }


  @GetMapping("/innsynskrav/{id}")
  public ResponseEntity<InnsynskravJSON> getInnsynskrav(
      @Valid @ExistingObject(type = Innsynskrav.class) @PathVariable String id,
      @Valid GetSingleRequestParameters params) {
    var innsynskrav = innsynskravService.findById(id);
    var expandFields = params.getExpand();
    if (expandFields == null) {
      return ResponseEntity.ok(innsynskravService.toJSON(innsynskrav));
    } else {
      return ResponseEntity.ok(innsynskravService.toJSON(innsynskrav, expandFields));
    }
  }


  @GetMapping("/innsynskrav/{id}/verify/{verificationSecret}")
  public ResponseEntity<InnsynskravJSON> verifyInnsynskrav(
      @Valid @ExistingObject(type = Innsynskrav.class) @PathVariable String id,
      @Valid @PathVariable String verificationSecret, @Valid GetSingleRequestParameters params)
      throws UnauthorizedException {
    Innsynskrav innsynskrav = innsynskravService.findById(id);

    // Verify
    InnsynskravJSON updatedInnsynskravJSON =
        innsynskravService.verify(innsynskrav, verificationSecret, params.getExpand());
    return ResponseEntity.ok(updatedInnsynskravJSON);
  }


}
