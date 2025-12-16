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

/** Represents a participant in a meeting. */
@Getter
@Setter
public class MoetedeltakerDTO extends ArkivBaseDTO {
  protected final String entity = "Moetedeltaker";

  /** The name of the meeting participant. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String moetedeltakerNavn;

  /** The function or role of the participant in the meeting (e.g., 'Chairperson'). */
  @NoSSN
  @Size(max = 500)
  protected String moetedeltakerFunksjon;
}
