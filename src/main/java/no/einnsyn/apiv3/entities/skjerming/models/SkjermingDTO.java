// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.skjerming.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;

@Getter
@Setter
public class SkjermingDTO extends ArkivBaseDTO {

  @Size(max = 500)
  final String entity = "Skjerming";

  @Size(max = 500)
  @NoSSN
  String skjermingshjemmel;

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = {Insert.class})
  String tilgangsrestriksjon;
}
