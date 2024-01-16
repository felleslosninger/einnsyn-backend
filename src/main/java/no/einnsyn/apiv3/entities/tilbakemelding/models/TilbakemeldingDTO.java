// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.tilbakemelding.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;

@Getter
@Setter
public class TilbakemeldingDTO extends BaseDTO {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
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

  @Null(groups = {Insert.class, Update.class})
  Boolean handledByAdmin;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String adminComment;
}
