// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.tilbakemelding.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;

@Getter
@Setter
public class TilbakemeldingDTO extends BaseDTO {

  @Size(max = 500)
  final String entity = "Tilbakemelding";

  @Size(max = 500)
  @NoSSN
  String messageFromUser;

  @Size(max = 500)
  @NoSSN
  String path;

  @Size(max = 500)
  @NoSSN
  String referer;

  @Size(max = 500)
  @NoSSN
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

  @Size(max = 500)
  @NoSSN
  String adminComment;
}
