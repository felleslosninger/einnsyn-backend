// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.tilbakemelding.models;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.validation.nossn.NoSSN;

/** Represents user feedback submitted through the application. */
@Getter
@Setter
public class TilbakemeldingDTO extends BaseDTO {
  protected final String entity = "Tilbakemelding";

  /** The feedback message from the user. */
  @NoSSN
  @Size(max = 500)
  protected String messageFromUser;

  /** The path of the page where the feedback was submitted. */
  @NoSSN
  @Size(max = 500)
  protected String path;

  /** The referer URL. */
  @NoSSN
  @Size(max = 500)
  protected String referer;

  /** The user agent string of the user's browser. */
  @NoSSN
  @Size(max = 500)
  protected String userAgent;

  /** The screen height of the user's device. */
  protected Integer screenHeight;

  /** The screen width of the user's device. */
  protected Integer screenWidth;

  /** The document height of the page. */
  protected Integer docHeight;

  /** The document width of the page. */
  protected Integer docWidth;

  /** The window height of the browser. */
  protected Integer winHeight;

  /** The window width of the browser. */
  protected Integer winWidth;

  /** The horizontal scroll position. */
  protected Integer scrollX;

  /** The vertical scroll position. */
  protected Integer scrollY;

  /** Indicates whether the user was satisfied. */
  protected Boolean userSatisfied;

  /** Indicates whether the feedback has been handled by an administrator. */
  protected Boolean handledByAdmin;

  /** A comment from the administrator who handled the feedback. */
  @NoSSN
  @Size(max = 500)
  protected String adminComment;
}
