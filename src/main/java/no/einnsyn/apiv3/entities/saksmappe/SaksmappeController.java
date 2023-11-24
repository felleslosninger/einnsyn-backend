package no.einnsyn.apiv3.entities.saksmappe;

import java.util.List;
import org.springframework.data.domain.Page;
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
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostGetListRequestParameters;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.requests.GetSingleRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;

@RestController
public class SaksmappeController {

  private final SaksmappeService saksmappeService;
  private final JournalpostService journalpostService;


  SaksmappeController(SaksmappeService saksmappeService, JournalpostService journalpostService) {
    this.saksmappeService = saksmappeService;
    this.journalpostService = journalpostService;
  }


  @GetMapping("/saksmappe")
  public ResponseEntity<ResponseList<SaksmappeJSON>> getSaksmappeList(
      @Valid GetListRequestParameters params) {

    var response = saksmappeService.list(params);
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
    return new ResponseEntity<>(createdSaksmappe, headers, HttpStatus.CREATED);
  }


  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> getSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id,
      @Valid GetSingleRequestParameters params) {
    var saksmappe = saksmappeService.findById(id);
    var expandFields = params.getExpand();
    if (expandFields == null) {
      return ResponseEntity.ok(saksmappeService.toJSON(saksmappe));
    } else {
      return ResponseEntity.ok(saksmappeService.toJSON(saksmappe, expandFields));
    }
  }


  @PutMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> updateSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id,
      @Validated({Update.class}) @NewObject @RequestBody SaksmappeJSON saksmappeJSON) {

    SaksmappeJSON updatedSaksmappe = saksmappeService.update(id, saksmappeJSON);
    return ResponseEntity.ok(updatedSaksmappe);
  }


  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> deleteSaksmappe(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String id) {
    SaksmappeJSON result = saksmappeService.delete(id);
    return ResponseEntity.ok(result);
  }


  @GetMapping("/saksmappe/{saksmappeId}/journalpost")
  public ResponseEntity<ResponseList<JournalpostJSON>> getSaksmappeJournalposts(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String saksmappeId,
      @Valid JournalpostGetListRequestParameters params) {

    params.setSaksmappeId(saksmappeId);
    Page<Journalpost> responsePage = journalpostService.getPage(params);
    ResponseList<JournalpostJSON> response = journalpostService.list(params, responsePage);
    return ResponseEntity.ok(response);
  }


  @PostMapping("/saksmappe/{saksmappeId}/journalpost")
  public ResponseEntity<SaksmappeJSON> createSaksmappeJournalposts(
      @Valid @ExistingObject(type = Saksmappe.class) @PathVariable String saksmappeId,
      @Validated(Insert.class) @NewObject @RequestBody ExpandableField<JournalpostJSON> journalpostField,
      HttpServletRequest request) {

    SaksmappeJSON saksmappeJSON = new SaksmappeJSON();
    saksmappeJSON.setJournalpost(List.of(journalpostField));
    SaksmappeJSON createdSaksmappe = saksmappeService.update(saksmappeId, saksmappeJSON);

    String saksmappeUrl = request.getRequestURL().toString() + "/" + createdSaksmappe.getId();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", saksmappeUrl);
    return new ResponseEntity<>(createdSaksmappe, headers, HttpStatus.CREATED);
  }

}
