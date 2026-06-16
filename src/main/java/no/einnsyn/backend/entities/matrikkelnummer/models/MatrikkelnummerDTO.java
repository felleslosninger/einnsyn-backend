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

@Getter
@Setter
public class MatrikkelnummerDTO extends ArkivBaseDTO {
  protected final String entity = "Matrikkelnummer";

  @Pattern(regexp = "^[0-9]{4}$")
  @NotBlank(groups = {Insert.class})
  protected String kommunenummer;

  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer gaardsnummer;

  @Min(0)
  @NotNull(groups = {Insert.class})
  protected Integer bruksnummer;

  @Min(0)
  protected Integer festenummer;

  @Min(0)
  protected Integer seksjonsnummer;
}
