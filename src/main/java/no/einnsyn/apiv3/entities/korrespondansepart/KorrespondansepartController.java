package no.einnsyn.apiv3.entities.korrespondansepart;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KorrespondansepartController {

  private final KorrespondansepartService korrespondansepartService;

  public KorrespondansepartController(KorrespondansepartService korrespondansepartService) {
    this.korrespondansepartService = korrespondansepartService;
  }

  @GetMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartJSON> getKorrespondansepart(
      @Valid @ExistingObject(type = Korrespondansepart.class) @PathVariable String id) {

    var object = korrespondansepartService.findById(id);
    var json = korrespondansepartService.toJSON(object);
    return ResponseEntity.ok(json);
  }

  @DeleteMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartJSON> deleteKorrespondansepart(
      @Valid @ExistingObject(type = Korrespondansepart.class) @PathVariable String id) {

    KorrespondansepartJSON response = korrespondansepartService.delete(id);
    return ResponseEntity.ok(response);
  }
}
