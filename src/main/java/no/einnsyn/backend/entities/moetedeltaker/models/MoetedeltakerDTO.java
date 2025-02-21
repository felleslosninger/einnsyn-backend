// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetedeltaker.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

/** Moetedeltaker */
@Getter
@Setter
public class MoetedeltakerDTO extends ArkivBaseDTO {
  protected final String entity = "Moetedeltaker";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String moetedeltakerNavn;

  @NoSSN
  @Size(max = 500)
  protected String moetedeltakerFunksjon;
}
