// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.moetedeltaker.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;

@Getter
@Setter
public class MoetedeltakerDTO extends ArkivBaseDTO {

  @Size(max = 500)
  @Null(groups = { Insert.class, Update.class })
  private final String entity = "Moetedeltaker";

  @Size(max = 500)
  @NoSSN
  @NotNull(groups = { Insert.class })
  private String moetedeltakerNavn;

  @Size(max = 500)
  @NoSSN
  private String moetedeltakerFunksjon;
}
