package no.einnsyn.apiv3.entities.saksmappe;

import java.util.Set;
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
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

@RestController
public class SaksmappeController {

  private final SaksmappeService saksmappeService;
  private final SaksmappeRepository saksmappeRepository;
  private final JournalpostService journalpostService;


  SaksmappeController(SaksmappeService saksmappeService, SaksmappeRepository saksmappeRepository,
      JournalpostService journalpostService) {
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
    this.journalpostService = journalpostService;
  }


  @GetMapping("/saksmappe")
  public ResponseEntity<ResponseList<SaksmappeJSON>> getSaksmappeList(
      @Valid GetListRequestParameters params) {

    ResponseList<SaksmappeJSON> response = saksmappeService.list(params);
    return ResponseEntity.ok(response);
  }


  @PostMapping("/saksmappe")
  public ResponseEntity<SaksmappeJSON> createSaksmappe(
      @Validated(Insert.class) @RequestBody SaksmappeJSON saksmappeJSON,
      HttpServletRequest request) {

    SaksmappeJSON createdSaksmappe = saksmappeService.update(null, saksmappeJSON);
    String saksmappeUrl = request.getRequestURL().toString() + "/" + createdSaksmappe.getId();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", saksmappeUrl);
    return new ResponseEntity<SaksmappeJSON>(createdSaksmappe, headers, HttpStatus.CREATED);
  }


  @PutMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> updateSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id,
      @Validated({Update.class}) @RequestBody SaksmappeJSON saksmappeJSON) {

    SaksmappeJSON updatedSaksmappe = saksmappeService.update(id, saksmappeJSON);
    return ResponseEntity.ok(updatedSaksmappe);
  }


  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> getSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id,
      @Valid GetListRequestParameters params) {
    Saksmappe saksmappe = saksmappeRepository.findById(id);
    Set<String> expandFields = params.getExpand();
    if (expandFields == null) {
      return ResponseEntity.ok(saksmappeService.toJSON(saksmappe));
    } else {
      return ResponseEntity.ok(saksmappeService.toJSON(saksmappe, expandFields));
    }

  }


  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> deleteSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id) {
    SaksmappeJSON result = saksmappeService.delete(id);
    return ResponseEntity.ok(result);
  }


  @GetMapping("/saksmappe/{id}/journalpost")
  public ResponseEntity<ResponseList<JournalpostJSON>> getSaksmappeJournalposts(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id,
      @Valid GetListRequestParameters params) {
    ResponseList<JournalpostJSON> response = journalpostService.list(params);
    return ResponseEntity.ok(response);
  }

}
