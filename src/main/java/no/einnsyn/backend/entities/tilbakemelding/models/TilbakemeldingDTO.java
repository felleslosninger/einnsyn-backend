// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

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
  final String entity = "Tilbakemelding";

  @NoSSN
  @Size(max = 500)
  String messageFromUser;

  @NoSSN
  @Size(max = 500)
  String path;

  @NoSSN
  @Size(max = 500)
  String referer;

  @NoSSN
  @Size(max = 500)
  String userAgent;

  Integer screenHeight;

  Integer screenWidth;

  Integer docHeight;

  Integer docWidth;

  Integer winHeight;

  Integer winWidth;

  Integer scrollX;

  Integer scrollY;

  Boolean userSatisfied;

  Boolean handledByAdmin;

  @NoSSN
  @Size(max = 500)
  String adminComment;
}
