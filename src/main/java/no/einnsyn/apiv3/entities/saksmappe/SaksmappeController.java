package no.einnsyn.apiv3.entities.saksmappe;

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
  public ResponseEntity<Saksmappe> createSaksmappe(
      @Validated(InsertValidationGroup.class) @RequestBody SaksmappeJSON saksmappeJSON) {
    try {
      Saksmappe createdSaksmappe = saksmappeService.updateSaksmappe(null, saksmappeJSON);
      return ResponseEntity.ok(createdSaksmappe);
    } catch (Error e) {
      // TODO: Log error and return correct error message
      System.out.println(e.getMessage());
      return ResponseEntity.badRequest().build();
    }
  }


  @PutMapping("/saksmappe/{id}")
  @Validated({UpdateValidationGroup.class})
  public ResponseEntity<Saksmappe> updateSaksmappe(@PathVariable String id,
      @RequestBody SaksmappeJSON saksmappeJSON) {
    try {
      Saksmappe updatedSaksmappe = saksmappeService.updateSaksmappe(id, saksmappeJSON);
      return ResponseEntity.ok(updatedSaksmappe);
    } catch (Error e) {
      // TODO: Log error and return correct error message
      return ResponseEntity.badRequest().build();
    }
  }


  /**
   * 
   */
  @GetMapping("/saksmappe/{id}")
  public ResponseEntity<Saksmappe> getSaksmappe(@RequestParam String externalId,
      @RequestParam String id) {
    Saksmappe saksmappe = null;

    if (id != null) {
      saksmappe = saksmappeRepository.findById(id).orElse(null);
    } else if (externalId != null) {
      saksmappe = saksmappeRepository.findByExternalId(externalId).orElse(null);
    }

    if (saksmappe == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(saksmappe);
  }


  @DeleteMapping("/saksmappe/{id}")
  public ResponseEntity<Boolean> deleteSaksmappe(@RequestParam String externalId,
      @RequestParam String id) {
    Boolean deleted = null;
    deleted = saksmappeService.deleteSaksmappe(id, externalId);
    if (deleted == null || !deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(deleted);
  }

}
