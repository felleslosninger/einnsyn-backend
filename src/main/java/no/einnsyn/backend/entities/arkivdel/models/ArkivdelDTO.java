// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.arkivdel.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkiv.ArkivService;
import no.einnsyn.backend.entities.arkiv.models.ArkivDTO;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class ArkivdelDTO extends ArkivBaseDTO {
  final String entity = "Arkivdel";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  String tittel;

  @ExpandableObject(
      service = ArkivService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<ArkivDTO> arkiv;
}
