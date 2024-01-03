package no.einnsyn.apiv3.entities.bruker;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.bruker.models.Bruker;
import no.einnsyn.apiv3.entities.bruker.models.BrukerJSON;
import no.einnsyn.apiv3.entities.bruker.models.SetPasswordWithOldPasswordRequestBody;
import no.einnsyn.apiv3.entities.bruker.models.SetPasswordWithSecretRequestBody;
import no.einnsyn.apiv3.exceptions.UnauthorizedException;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BrukerController {

  private final BrukerService brukerService;

  public BrukerController(BrukerService brukerService) {
    this.brukerService = brukerService;
  }

  @PostMapping("/bruker")
  public ResponseEntity<BrukerJSON> createBruker(
      @RequestBody @Validated(Insert.class) @NewObject BrukerJSON brukerJSON,
      HttpServletRequest request) {
    var response = brukerService.update(brukerJSON);
    var url = request.getRequestURL().toString() + "/" + response.getId();
    var headers = new HttpHeaders();
    headers.add("Location", url);
    return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
  }

  @GetMapping("/bruker/{id}")
  @PreAuthorize("@preAuth.isSelf(#id)")
  public ResponseEntity<BrukerJSON> getBruker(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id) {
    var bruker = brukerService.findById(id);
    return ResponseEntity.ok(brukerService.toJSON(bruker));
  }

  @PutMapping("/bruker/{id}")
  public ResponseEntity<BrukerJSON> updateBruker(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id,
      @RequestBody @Validated(Update.class) BrukerJSON brukerJSON,
      HttpServletRequest request) {
    var response = brukerService.update(id, brukerJSON);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/bruker/{id}")
  public ResponseEntity<BrukerJSON> deleteBruker(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id) {
    var bruker = brukerService.delete(id);
    return ResponseEntity.ok(bruker);
  }

  /**
   * Activate bruker
   *
   * @param id
   * @param secret
   * @return
   * @throws UnauthorizedException
   */
  @PostMapping("/bruker/{id}/activate/{secret}")
  public ResponseEntity<BrukerJSON> activateBruker(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id,
      @PathVariable String secret)
      throws UnauthorizedException {
    var bruker = brukerService.findById(id);
    var brukerJSON = brukerService.activate(bruker, secret);
    return ResponseEntity.ok(brukerJSON);
  }

  // Update password, verified by old password
  @PutMapping("/bruker/{id}/updatePassword")
  public ResponseEntity<BrukerJSON> updatePassword(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id,
      @RequestBody @Valid SetPasswordWithOldPasswordRequestBody body,
      HttpServletRequest request)
      throws UnauthorizedException {
    var bruker = brukerService.findById(id);
    var response =
        brukerService.updatePasswordWithOldPassword(
            bruker, body.getOldPassword(), body.getNewPassword());
    return ResponseEntity.ok(response);
  }

  // Update password, verified by secret
  @PutMapping("/bruker/{id}/updatePassword/{secret}")
  public ResponseEntity<BrukerJSON> updatePasswordWithSecret(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id,
      @Valid @PathVariable String secret,
      @RequestBody @Valid SetPasswordWithSecretRequestBody body,
      HttpServletRequest request)
      throws UnauthorizedException {
    var bruker = brukerService.findById(id);
    var response = brukerService.updatePasswordWithSecret(bruker, secret, body.getNewPassword());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/bruker/{id}/requestPasswordReset")
  public ResponseEntity<BrukerJSON> requestPasswordReset(
      @Valid @ExistingObject(type = Bruker.class) @PathVariable String id)
      throws MessagingException {
    var bruker = brukerService.findById(id);
    brukerService.requestPasswordReset(bruker);
    return ResponseEntity.ok(brukerService.toJSON(bruker));
  }
}
