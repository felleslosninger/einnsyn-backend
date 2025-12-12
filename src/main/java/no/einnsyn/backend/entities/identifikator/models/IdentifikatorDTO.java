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

/** Represents an identifier for a person, such as a case officer or an author. */
@Getter
@Setter
public class IdentifikatorDTO extends ArkivBaseDTO {
  protected final String entity = "Identifikator";

  /** The full name of the person. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String navn;

  /** A unique identifier for the person, often a username or an employee ID. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String identifikator;

  /** The initials of the person. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String initialer;

  /** The email address of the person. */
  @Email
  @NotBlank(groups = {Insert.class})
  protected String epostadresse;
}
