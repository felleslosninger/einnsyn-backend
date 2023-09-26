package no.einnsyn.apiv3.entities.journalpost;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;

@RestController
public class JournalpostController {

  private final JournalpostService journalpostService;


  JournalpostController(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostJSON> deleteJournalpost(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id) {
    JournalpostJSON result = journalpostService.delete(id);
    return ResponseEntity.ok(result);
  }
}
