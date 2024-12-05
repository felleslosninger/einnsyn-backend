// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.votering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.identifikator.IdentifikatorService;
import no.einnsyn.backend.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.backend.entities.moetedeltaker.MoetedeltakerService;
import no.einnsyn.backend.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class VoteringDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Votering";

  @ExpandableObject(
      service = MoetedeltakerService.class,
      groups = {Insert.class, Update.class})
  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetedeltakerDTO> moetedeltaker;

  @Size(max = 500)
  @ValidEnum(enumClass = StemmeEnum.class)
  @NotBlank(groups = {Insert.class})
  String stemme;

  @ExpandableObject(
      service = IdentifikatorService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<IdentifikatorDTO> representerer;
}
