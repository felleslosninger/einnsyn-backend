package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.transaction.Transactional;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.tilbakemelding.models.Tilbakemelding;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingJSON;
import org.springframework.stereotype.Service;

@Service
public class TilbakemeldingService
    extends EinnsynObjectService<Tilbakemelding, TilbakemeldingJSON> {

  @Getter private final TilbakemeldingRepository repository;

  @Getter private TilbakemeldingService service = this;

  TilbakemeldingService(TilbakemeldingRepository repository) {
    this.repository = repository;
  }

  public Tilbakemelding newObject() {
    return new Tilbakemelding();
  }

  public TilbakemeldingJSON newJSON() {
    return new TilbakemeldingJSON();
  }

  // Data from front-end
  @Override
  public Tilbakemelding fromJSON(
      TilbakemeldingJSON json,
      Tilbakemelding tilbakemelding,
      Set<String> paths,
      String currentPath) {
    super.fromJSON(json, tilbakemelding, paths, currentPath);

    if (json.getMessageFromUser() != null) {
      tilbakemelding.setMessageFromUser(json.getMessageFromUser());
    }

    if (json.getPath() != null) {
      tilbakemelding.setPath(json.getPath());
    }

    if (json.getReferer() != null) {
      tilbakemelding.setReferer(json.getReferer());
    }

    if (json.getUserAgent() != null) {
      tilbakemelding.setUserAgent(json.getUserAgent());
    }

    if (json.getScreenHeight() != null) {
      tilbakemelding.setScreenHeight(json.getScreenHeight());
    }

    if (json.getScreenWidth() != null) {
      tilbakemelding.setScreenWidth(json.getScreenWidth());
    }

    if (json.getDocHeight() != null) {
      tilbakemelding.setDocHeight(json.getDocHeight());
    }

    if (json.getDocWidth() != null) {
      tilbakemelding.setDocWidth(json.getDocWidth());
    }

    if (json.getWinHeight() != null) {
      tilbakemelding.setWinHeight(json.getWinHeight());
    }

    if (json.getWinWidth() != null) {
      tilbakemelding.setWinWidth(json.getWinWidth());
    }

    if (json.getScrollX() != null) {
      tilbakemelding.setScrollX(json.getScrollX());
    }

    if (json.getScrollY() != null) {
      tilbakemelding.setScrollY(json.getScrollY());
    }

    if (json.getUserSatisfied() != null) {
      tilbakemelding.setUserSatisfied(json.getUserSatisfied());
    }

    if (json.getHandledByAdmin() != null) {
      tilbakemelding.setHandledByAdmin(json.getHandledByAdmin());
    }

    if (json.getAdminComment() != null) {
      tilbakemelding.setAdminComment(json.getAdminComment());
    }

    return tilbakemelding;
  }

  // Data to front-end
  @Override
  public TilbakemeldingJSON toJSON(
      Tilbakemelding tilbakemelding,
      TilbakemeldingJSON json,
      Set<String> expandPaths,
      String currentPath) {
    super.toJSON(tilbakemelding, json, expandPaths, currentPath);

    json.setMessageFromUser((tilbakemelding.getMessageFromUser()));
    json.setPath((tilbakemelding.getPath()));
    json.setReferer((tilbakemelding.getReferer()));
    json.setUserAgent((tilbakemelding.getUserAgent()));
    json.setScreenHeight((tilbakemelding.getScreenHeight()));
    json.setScreenWidth((tilbakemelding.getScreenWidth()));
    json.setDocHeight((tilbakemelding.getDocHeight()));
    json.setDocWidth((tilbakemelding.getDocWidth()));
    json.setWinHeight((tilbakemelding.getWinHeight()));
    json.setWinWidth((tilbakemelding.getWinWidth()));
    json.setScrollX((tilbakemelding.getScrollX()));
    json.setScrollY((tilbakemelding.getScrollY()));
    json.setUserSatisfied((tilbakemelding.isUserSatisfied()));
    json.setHandledByAdmin((tilbakemelding.isHandledByAdmin()));
    json.setAdminComment((tilbakemelding.getAdminComment()));

    return json;
  }

  /**
   * Delete an tilbakemelding
   *
   * @param tilbakemelding
   * @return
   */
  @Transactional
  public TilbakemeldingJSON delete(Tilbakemelding tilbakemelding) {
    TilbakemeldingJSON tilbakemeldingJSON = toJSON(tilbakemelding);
    tilbakemeldingJSON.setDeleted(true);
    repository.delete(tilbakemelding);

    return tilbakemeldingJSON;
  }
}
