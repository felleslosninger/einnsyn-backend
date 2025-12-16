// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.behandlingsprotokoll.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

/** Represents a record of proceedings, often related to a decision-making process in a meeting. */
@Getter
@Setter
public class BehandlingsprotokollDTO extends ArkivBaseDTO {
  protected final String entity = "Behandlingsprotokoll";

  /** The content of the protocol. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tekstInnhold;

  /** The format of the content (e.g., "text/html"). */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tekstFormat;
}
