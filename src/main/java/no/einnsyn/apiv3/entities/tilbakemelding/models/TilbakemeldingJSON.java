package no.einnsyn.apiv3.entities.tilbakemelding.models;

import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObjectJSON;
import java.net.URL;

@Getter
@Setter
public class TilbakemeldingJSON extends EinnsynObjectJSON {
  private String messageFromUser;
  private URL path;
  private String referer;
  private String userAgent;
  private Integer screenHeight;
  private Integer screenWidth;
  private Integer docHeight;
  private Integer docWidth;
  private Integer winHeight;
  private Integer winWidth;
  private Integer scrollX;
  private Integer scrollY;
  private Boolean userSatisfied;
  private Boolean handledByAdmin;
  private String adminComment;
}
