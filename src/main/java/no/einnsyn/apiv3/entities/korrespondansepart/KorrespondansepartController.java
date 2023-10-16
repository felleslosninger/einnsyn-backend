package no.einnsyn.apiv3.entities.korrespondansepart;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;

@RestController
public class KorrespondansepartController {


  private final KorrespondansepartRepository korrespondansepartRepository;

  private final KorrespondansepartService korrespondansepartService;


  public KorrespondansepartController(KorrespondansepartRepository korrespondansepartRepository,
      KorrespondansepartService korrespondansepartService) {
    this.korrespondansepartRepository = korrespondansepartRepository;
    this.korrespondansepartService = korrespondansepartService;
  }


  @GetMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartJSON> getKorrespondansepart(
      @Valid @ExistingObject(type = Korrespondansepart.class) @PathVariable String id) {

    Korrespondansepart object = korrespondansepartRepository.findById(id);
    KorrespondansepartJSON json = korrespondansepartService.toJSON(object);
    return ResponseEntity.ok(json);
  }


  @DeleteMapping("/korrespondansepart/{id}")
  public ResponseEntity<KorrespondansepartJSON> deleteKorrespondansepart(
      @Valid @ExistingObject(type = Korrespondansepart.class) @PathVariable String id) {

    KorrespondansepartJSON response = korrespondansepartService.delete(id);
    return ResponseEntity.ok(response);
  }


}
