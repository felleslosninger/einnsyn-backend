package no.einnsyn.apiv3.entities.saksmappe;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;
import no.einnsyn.apiv3.validationGroups.InsertValidationGroup;
import no.einnsyn.apiv3.validationGroups.UpdateValidationGroup;

@RestController
@Validated
public class SaksmappeController {

  private final SaksmappeService saksmappeService;
  private final SaksmappeRepository saksmappeRepository;


  SaksmappeController(SaksmappeService saksmappeService, SaksmappeRepository saksmappeRepository) {
    this.saksmappeService = saksmappeService;
    this.saksmappeRepository = saksmappeRepository;
  }


  @PostMapping("/saksmappe")
  public ResponseEntity<SaksmappeJSON> createSaksmappe(
      @Validated(InsertValidationGroup.class) @RequestBody SaksmappeJSON saksmappeJSON,
      HttpServletRequest request) {
    try {
      Saksmappe createdSaksmappe = saksmappeService.updateSaksmappe(null, saksmappeJSON);
      String saksmappeUrl = request.getRequestURL().toString() + "/" + createdSaksmappe.getId();
      HttpHeaders headers = new HttpHeaders();
      headers.add("Location", saksmappeUrl);
      return new ResponseEntity<SaksmappeJSON>(saksmappeService.toJSON(createdSaksmappe, 2),
          headers, HttpStatus.CREATED);
    } catch (Error e) {
      // TODO: Log error and return correct error message
      return ResponseEntity.badRequest().build();
    }
  }


  @PutMapping("/saksmappe/{id}")
  @Validated({UpdateValidationGroup.class})
  public ResponseEntity<SaksmappeJSON> updateSaksmappe(@PathVariable String id,
      @RequestBody SaksmappeJSON saksmappeJSON) {
    try {
      Saksmappe updatedSaksmappe = saksmappeService.updateSaksmappe(id, saksmappeJSON);
      return ResponseEntity.ok(saksmappeService.toJSON(updatedSaksmappe, 2));
    } catch (Error e) {
      // TODO: Log error and return correct error message
      return ResponseEntity.badRequest().build();
    }
  }


  /**
   * 
   */
  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<SaksmappeJSON> getSaksmappe(@RequestParam String id) {
    try {
      Saksmappe saksmappe = saksmappeRepository.findById(id).orElse(null);
      return ResponseEntity.ok(saksmappeService.toJSON(saksmappe, 2));
    } catch (Error e) {
      // TODO: Improve error handling
      return ResponseEntity.notFound().build();
    }

  }


  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<Boolean> deleteSaksmappe(@RequestParam String externalId,
      @RequestParam String id) {
    Boolean deleted = null;
    try {
      saksmappeService.deleteSaksmappe(id, externalId);
    } catch (Error e) {
      // TODO: Improve error handling
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(deleted);
  }

}
