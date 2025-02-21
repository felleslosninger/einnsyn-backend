// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.tilbakemelding.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;

/** Tilbakemelding */
@Getter
@Setter
public class TilbakemeldingDTO extends BaseDTO {
  protected final String entity = "Tilbakemelding";

  @NoSSN
  @Size(max = 500)
  protected String messageFromUser;

  @NoSSN
  @Size(max = 500)
  protected String path;

  @NoSSN
  @Size(max = 500)
  protected String referer;

  @NoSSN
  @Size(max = 500)
  protected String userAgent;

  protected Integer screenHeight;

  protected Integer screenWidth;

  protected Integer docHeight;

  protected Integer docWidth;

  protected Integer winHeight;

  protected Integer winWidth;

  protected Integer scrollX;

  protected Integer scrollY;

  protected Boolean userSatisfied;

  protected Boolean handledByAdmin;

  @NoSSN
  @Size(max = 500)
  protected String adminComment;
}
