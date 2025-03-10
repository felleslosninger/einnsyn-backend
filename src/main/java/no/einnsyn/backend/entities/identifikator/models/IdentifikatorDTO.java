// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.identifikator.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

/** Identifikator */
@Getter
@Setter
public class IdentifikatorDTO extends ArkivBaseDTO {
  protected final String entity = "Identifikator";

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String navn;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String identifikator;

  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String initialer;

  @Email
  @NotBlank(groups = {Insert.class})
  protected String epostadresse;
}
