package no.einnsyn.apiv3.entities.tilbakemelding.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.einnsynobject.models.EinnsynObject;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Entity
@DynamicUpdate
public class Tilbakemelding extends EinnsynObject {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilbakemelding_seq")
  @SequenceGenerator(name = "tilbakemelding_seq", sequenceName = "tilbakemelding_seq", allocationSize = 1)
  private Integer tilbakemeldingId;
  private String messageFromUser;
  private String path;
  private String referer;
  private String userAgent;
  private Integer screenHeight;
  private Integer screenWidth;
  private Integer docHeight;
  private Integer docWidth;
  private Integer winHeight;
  private Integer winWidth;
  @Column(name = "scroll_x")
  private Integer scrollX;
  @Column(name = "scroll_y")
  private Integer scrollY;
  private boolean userSatisfied;
  private boolean handledByAdmin;
  private String adminComment;

}
