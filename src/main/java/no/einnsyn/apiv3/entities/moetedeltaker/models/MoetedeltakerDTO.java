// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedeltaker.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class MoetedeltakerDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Moetedeltaker";

  @Size(max = 500)
  @NoSSN
  @NotBlank(groups = {Insert.class})
  String moetedeltakerNavn;

  @Size(max = 500)
  @NoSSN
  String moetedeltakerFunksjon;
}
