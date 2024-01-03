package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DokumentbeskrivelseController {

  DokumentbeskrivelseService dokumentbeskrivelseService;

  DokumentbeskrivelseController(DokumentbeskrivelseService dokumentbeskrivelseService) {
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
  }

  @GetMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseJSON> getJournalpost(
      @Valid @ExistingObject(type = Dokumentbeskrivelse.class) @PathVariable String id) {

    Dokumentbeskrivelse object = dokumentbeskrivelseService.findById(id);
    DokumentbeskrivelseJSON json = dokumentbeskrivelseService.toJSON(object);
    return ResponseEntity.ok(json);
  }
}
