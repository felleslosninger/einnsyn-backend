package no.einnsyn.backend.entities.tilbakemelding.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.Base;

@Getter
@Setter
@Entity
public class Tilbakemelding extends Base {
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
