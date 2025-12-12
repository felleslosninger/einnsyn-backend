// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.moetesaksbeskrivelse.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

/**
 * Represents a textual description related to a meeting case, such as a recommendation or a report.
 */
@Getter
@Setter
public class MoetesaksbeskrivelseDTO extends ArkivBaseDTO {
  protected final String entity = "Moetesaksbeskrivelse";

  /** The text content of the description. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tekstInnhold;

  /** The format of the text content (e.g., "text/html"). */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tekstFormat;
}
