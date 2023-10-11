package no.einnsyn.apiv3.entities.innsynskrav;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskrav.models.InnsynskravJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;

@RestController
public class InnsynskravController {

  private final InnsynskravService innsynskravService;

  InnsynskravController(InnsynskravService innsynskravService) {
    this.innsynskravService = innsynskravService;
  }


  @PostMapping("/innsynskrav")
  public ResponseEntity<InnsynskravJSON> createInnsynskrav(
      @Validated(Insert.class) @NewObject @RequestBody InnsynskravJSON innsynskravJSON,
      HttpServletRequest request) {
    InnsynskravJSON createdInnsynskravJSON = innsynskravService.update(innsynskravJSON);

    // TODO: Add location header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<InnsynskravJSON>(createdInnsynskravJSON, headers, HttpStatus.CREATED);
  }


  @GetMapping("/innsynskrav/{id}/verify/{verificationSecret}")
  public ResponseEntity<InnsynskravJSON> verifyInnsynskrav(
      @Valid @ExistingObject(type = Innsynskrav.class) @PathVariable String id,
      @Valid @PathVariable String verificationSecret) {
    InnsynskravJSON updatedInnsynskravJSON = innsynskravService.verify(id, verificationSecret);
    return ResponseEntity.ok(updatedInnsynskravJSON);
  }


}
