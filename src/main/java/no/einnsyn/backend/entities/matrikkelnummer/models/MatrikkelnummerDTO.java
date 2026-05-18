// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.matrikkelnummer.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.validationgroups.Insert;

/** Represents a cadastral identifier entry owned by a journalenhet. */
@Getter
@Setter
public class MatrikkelnummerDTO extends ArkivBaseDTO {
  protected final String entity = "Matrikkelnummer";

  /** The municipality number for the cadastral entry. */
  @Pattern(regexp = "^[0-9]{4}$")
  @NotBlank(groups = {Insert.class})
  protected String kommunenummer;

  /** The cadastral farm number. */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer gaardsnummer;

  /** The cadastral usage number. */
  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer bruksnummer;

  /** The optional leasehold number. */
  @Min(0)
  protected Integer festenummer;

  /** The optional section number. */
  @Min(0)
  protected Integer seksjonsnummer;
}
