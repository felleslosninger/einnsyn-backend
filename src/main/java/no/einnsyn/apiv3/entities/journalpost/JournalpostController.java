package no.einnsyn.apiv3.entities.journalpost;

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
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.DokumentbeskrivelseService;
import no.einnsyn.apiv3.entities.dokumentbeskrivelse.models.DokumentbeskrivelseJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.KorrespondansepartService;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.JournalpostInsert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import no.einnsyn.apiv3.requests.GetSingleRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

@RestController
public class JournalpostController {

  private final JournalpostService journalpostService;
  private final JournalpostRepository journalpostRepository;
  private final DokumentbeskrivelseService dokumentbeskrivelseService;
  private final KorrespondansepartService korrespondansepartService;


  JournalpostController(JournalpostService journalpostService,
      JournalpostRepository journalpostRepository,
      DokumentbeskrivelseService dokumentbeskrivelseService,
      KorrespondansepartService korrespondansepartService) {
    this.journalpostService = journalpostService;
    this.journalpostRepository = journalpostRepository;
    this.dokumentbeskrivelseService = dokumentbeskrivelseService;
    this.korrespondansepartService = korrespondansepartService;
  }


  @GetMapping("/journalpost")
  public ResponseEntity<ResponseList<JournalpostJSON>> getJournalpostList(
      @Valid JournalpostGetListRequestParameters params) {

    ResponseList<JournalpostJSON> response = journalpostService.list(params);
    return ResponseEntity.ok(response);
  }


  @PostMapping("/journalpost")
  public ResponseEntity<JournalpostJSON> createJournalpost(
      @Validated({Insert.class,
          JournalpostInsert.class}) @NewObject @RequestBody JournalpostJSON journalpostJSON,
      HttpServletRequest request) {
    JournalpostJSON response = journalpostService.update(null, journalpostJSON);

    String url = request.getRequestURL().toString() + "/" + response.getId();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", url);
    return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
  }


  @GetMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostJSON> getJournalpost(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id,
      @Valid GetSingleRequestParameters params) {
    var journalpost = journalpostRepository.findById(id);
    var expandFields = params.getExpand();
    if (expandFields == null) {
      return ResponseEntity.ok(journalpostService.toJSON(journalpost));
    } else {
      return ResponseEntity.ok(journalpostService.toJSON(journalpost, expandFields));
    }
  }


  @PutMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostJSON> updateJournalpost(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id,
      @Validated({Update.class}) @NewObject @RequestBody JournalpostJSON journalpostJSON) {
    JournalpostJSON response = journalpostService.update(id, journalpostJSON);
    return ResponseEntity.ok(response);
  }


  @DeleteMapping("/journalpost/{id}")
  public ResponseEntity<JournalpostJSON> deleteJournalpost(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id) {
    JournalpostJSON result = journalpostService.delete(id);
    return ResponseEntity.ok(result);
  }


  @PostMapping("/journalpost/{id}/dokumentbeskrivelse")
  public ResponseEntity<DokumentbeskrivelseJSON> addDokumentbeskrivelse(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id,
      @Validated({Update.class}) @NewObject @RequestBody DokumentbeskrivelseJSON dokbeskJSON,
      HttpServletRequest request) {

    // Create Dokumentbeskrivelse
    DokumentbeskrivelseJSON insertedDokbeskJSON = dokumentbeskrivelseService.update(dokbeskJSON);

    // Relate Dokumentbeskrivelse to Journalpost
    JournalpostJSON journalpostJSON = new JournalpostJSON();
    journalpostJSON.setDokumentbeskrivelse(
        Arrays.asList(new ExpandableField<DokumentbeskrivelseJSON>(insertedDokbeskJSON.getId())));
    journalpostService.update(id, journalpostJSON);

    // TODO: Add `location` header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<>(insertedDokbeskJSON, headers, HttpStatus.CREATED);
  }


  @PostMapping("/journalpost/{id}/korrespondansepart")
  public ResponseEntity<KorrespondansepartJSON> addKorrespondansepart(
      @Valid @ExistingObject(type = Journalpost.class) @PathVariable String id,
      @Validated({Insert.class}) @NewObject @RequestBody KorrespondansepartJSON korrpartJSON,
      HttpServletRequest request) {

    // Create Korrespondansepart
    korrpartJSON.setJournalpost(new ExpandableField<>(id));
    KorrespondansepartJSON insertedKorrpartJSON = korrespondansepartService.update(korrpartJSON);

    // Relate Korrespondansepart to Journalpost
    JournalpostJSON journalpostJSON = new JournalpostJSON();
    journalpostJSON.setKorrespondansepart(
        Arrays.asList(new ExpandableField<KorrespondansepartJSON>(insertedKorrpartJSON)));
    journalpostService.update(id, journalpostJSON);

    // TODO: Add `location` header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<>(insertedKorrpartJSON, headers, HttpStatus.CREATED);

  }

  // Skjerming?
  // Saksmappe? (update / change)
}
