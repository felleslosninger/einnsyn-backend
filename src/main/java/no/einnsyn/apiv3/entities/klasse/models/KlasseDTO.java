// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.klasse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class KlasseDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Klasse";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String tittel;

  @Valid ExpandableField<KlasseParentDTO> parent;
}
