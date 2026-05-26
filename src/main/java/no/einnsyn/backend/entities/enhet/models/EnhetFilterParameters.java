// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.enhet.models;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.validation.nossn.NoSSN;

@Getter
@Setter
public class EnhetFilterParameters extends ListParameters {
  /**
   * Free-text filter against navn, navnNynorsk, navnEngelsk, navnSami, orgnummer and enhetskode.
   */
  @NoSSN
  @Size(max = 500)
  protected String query;

  /** Filter by exact orgnummer(s). */
  @Size(max = 100)
  protected List<String> orgnummer;
}
