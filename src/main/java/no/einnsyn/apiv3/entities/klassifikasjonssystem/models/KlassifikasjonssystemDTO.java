// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klassifikasjonssystem.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.arkivdel.ArkivdelService;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.validation.expandableobject.ExpandableObject;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class KlassifikasjonssystemDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Klassifikasjonssystem";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String tittel;

  @ExpandableObject(
      service = ArkivdelService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<ArkivdelDTO> parent;
}
