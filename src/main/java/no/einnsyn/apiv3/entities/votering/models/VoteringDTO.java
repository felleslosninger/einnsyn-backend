// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.votering.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.identifikator.models.IdentifikatorDTO;
import no.einnsyn.apiv3.entities.moetedeltaker.models.MoetedeltakerDTO;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import no.einnsyn.apiv3.validation.validenum.ValidEnum;

@Getter
@Setter
public class VoteringDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  final String entity = "Votering";

  @NotNull(groups = {Insert.class})
  @Valid
  ExpandableField<MoetedeltakerDTO> moetedeltaker;

  @Size(max = 500)
  @ValidEnum(enumClass = StemmeEnum.class)
  @NotNull(groups = {Insert.class})
  String stemme;

  @Valid ExpandableField<IdentifikatorDTO> representerer;
}
