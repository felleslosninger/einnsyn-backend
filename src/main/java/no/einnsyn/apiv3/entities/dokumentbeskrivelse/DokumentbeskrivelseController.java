package no.einnsyn.apiv3.entities.dokumentbeskrivelse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.Dokumentbeskrivelse;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;

@RestController
public class DokumentbeskrivelseController {

  DokumentbeskrivelseRepository dokumentbeskrivelseRepository;

  DokumentbeskrivelseService dokumentbeskrivelseService;

  DokumentbeskrivelseController(DokumentbeskrivelseRepository dokumentbeskrivelseRepository,
      DokumentbeskrivelseService dokumentbeskrivelseService) {
    this.dokumentbeskrivelseRepository = dokumentbeskrivelseRepository;
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
  }

  @GetMapping("/dokumentbeskrivelse/{id}")
  public ResponseEntity<DokumentbeskrivelseJSON> getJournalpost(
      @Valid @ExistingObject(type = Dokumentbeskrivelse.class) @PathVariable String id) {

    Dokumentbeskrivelse object = dokumentbeskrivelseRepository.findById(id);
    DokumentbeskrivelseJSON json = dokumentbeskrivelseService.toJSON(object);
    return ResponseEntity.ok(json);
  }

}
