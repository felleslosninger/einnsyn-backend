package no.einnsyn.backend.validation;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
@RequestMapping("/validation-tests")
class ValidationTestController {

  @PostMapping("/expandable/both")
  ResponseEntity<Void> validateMustExistAndMustNotExist(
      @RequestBody @Valid BothConstraintRequest body) {
    return ResponseEntity.ok().build();
  }

  @PostMapping("/expandable/must-exist")
  ResponseEntity<Void> validateMustExist(@RequestBody @Valid MustExistRequest body) {
    return ResponseEntity.ok().build();
  }

  @PostMapping("/expandable/must-not-exist")
  ResponseEntity<Void> validateMustNotExist(@RequestBody @Valid MustNotExistRequest body) {
    return ResponseEntity.ok().build();
  }

  @Getter
  @Setter
  static class BothConstraintRequest {
    @ExpandableObject(service = ArkivService.class, mustExist = true, mustNotExist = true)
    private String arkiv;
  }

  @Getter
  @Setter
  static class MustExistRequest {
    @ExpandableObject(service = ArkivService.class, mustExist = true)
    private ExpandableField<ArkivDTO> arkiv;
  }

  @Getter
  @Setter
  static class MustNotExistRequest {
    @ExpandableObject(service = ArkivService.class, mustNotExist = true)
    private ExpandableField<ArkivDTO> arkiv;
  }
}
