// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.skjerming.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;

/**
 * Represents access control information for a resource, specifying restrictions and the legal basis
 * for them.
 */
@Getter
@Setter
public class SkjermingDTO extends ArkivBaseDTO {
  protected final String entity = "Skjerming";

  /** The code for the access restriction. */
  @NoSSN
  @Size(max = 500)
  @NotBlank(groups = {Insert.class})
  protected String tilgangsrestriksjon;

  /** The legal basis for the access restriction (a reference to a law or regulation). */
  @NoSSN
  @Size(max = 500)
  protected String skjermingshjemmel;
}
