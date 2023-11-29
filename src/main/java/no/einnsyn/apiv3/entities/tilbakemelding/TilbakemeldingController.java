package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.einnsyn.apiv3.entities.tilbakemelding.models.Tilbakemelding;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingJSON;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.features.validation.ExistingObject.ExistingObject;
import no.einnsyn.apiv3.features.validation.NewObject.NewObject;
import no.einnsyn.apiv3.features.validation.validationGroups.Insert;
import no.einnsyn.apiv3.features.validation.validationGroups.Update;
import no.einnsyn.apiv3.requests.GetListRequestParameters;
import no.einnsyn.apiv3.responses.ResponseList;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
public class TilbakemeldingController {

  private final TilbakemeldingService tilbakemeldingService;
  private final TilbakemeldingRepository tilbakemeldingRepository;

  TilbakemeldingController(TilbakemeldingService tilbakemeldingService, TilbakemeldingRepository tilbakemeldingRepository) {
    this.tilbakemeldingService = tilbakemeldingService;
    this.tilbakemeldingRepository = tilbakemeldingRepository;
  }

  //Get only one "tilbakemelding"
  @GetMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingJSON> getTilbakemelding(
          @Valid @ExistingObject(type = Tilbakemelding.class) @PathVariable String id) {
    Tilbakemelding tilbakemelding = tilbakemeldingRepository.findById(id);
    TilbakemeldingJSON tilbakemeldingJSON = tilbakemeldingService.toJSON(tilbakemelding);
    return ResponseEntity.ok(tilbakemeldingJSON);
  }

  //Get multiple "tilbakemelding"
  @GetMapping("/tilbakemelding")
  public ResponseEntity<ResponseList<TilbakemeldingJSON>> getTilbakemeldingList(
      @Valid GetListRequestParameters params) {
    ResponseList<TilbakemeldingJSON> response = tilbakemeldingService.list(params);
    return ResponseEntity.ok(response);
  }

  //Receive and store "tilbakemelding"
  @PostMapping("/tilbakemelding")
  public ResponseEntity<TilbakemeldingJSON> createTilbakemelding(
      @Validated(Insert.class) @NewObject @RequestBody TilbakemeldingJSON tilbakemeldingJSON,
      HttpServletRequest request) {
    TilbakemeldingJSON createdTilbakemelding = tilbakemeldingService.update(tilbakemeldingJSON);

    // TODO: Add location header
    HttpHeaders headers = new HttpHeaders();
    return new ResponseEntity<TilbakemeldingJSON>(createdTilbakemelding, headers, HttpStatus.CREATED);
  }

  //Update "tilbakemelding"
  @PutMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingJSON> updateTilbakemelding(
      @Valid @ExistingObject(type = Tilbakemelding.class) @PathVariable String id,
      @Validated(Update.class) @NewObject @RequestBody TilbakemeldingJSON tilbakemeldingJSON) {
    TilbakemeldingJSON updatedTilbakemelding = tilbakemeldingService.update(id, tilbakemeldingJSON);
    return ResponseEntity.ok(updatedTilbakemelding);
  }

  //Delete "tilbakemelding"
  @DeleteMapping("/tilbakemelding/{id}")
  public ResponseEntity<TilbakemeldingJSON> deleteTilbakemelding(
      @Valid @ExistingObject(type = Tilbakemelding.class) @PathVariable String id) {
    TilbakemeldingJSON deletedTilbakemeldingJSON = tilbakemeldingService.delete(id);
    return ResponseEntity.ok(deletedTilbakemeldingJSON);
  }

}
