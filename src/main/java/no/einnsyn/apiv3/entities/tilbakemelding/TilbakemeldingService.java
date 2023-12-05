package no.einnsyn.apiv3.entities.tilbakemelding;

import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.tilbakemelding.models.Tilbakemelding;
import no.einnsyn.apiv3.entities.tilbakemelding.models.TilbakemeldingJSON;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
public class TilbakemeldingService extends EinnsynObjectService<Tilbakemelding, TilbakemeldingJSON> {

  @Getter
  private final TilbakemeldingRepository repository;

  @Getter
  private TilbakemeldingService service = this;

  TilbakemeldingService(TilbakemeldingRepository repository) {
    this.repository = repository;
  }

  public Tilbakemelding newObject() {
    return new Tilbakemelding();
  }

  public TilbakemeldingJSON newJSON() {
    return new TilbakemeldingJSON();
  }

  //Data from front-end
  @Override
  public Tilbakemelding fromJSON(TilbakemeldingJSON json, Tilbakemelding Tilbakemelding, Set<String> paths, String currentPath) {
    super.fromJSON(json, Tilbakemelding, paths, currentPath);

    if (json.getMessageFromUser() != null) {
      Tilbakemelding.setMessageFromUser(json.getMessageFromUser());
    }

    if (json.getPath() != null) {
      Tilbakemelding.setPath(json.getPath());
    }

    if (json.getReferer() != null) {
      Tilbakemelding.setReferer(json.getReferer());
    }

    if (json.getUserAgent() != null) {
      Tilbakemelding.setUserAgent(json.getUserAgent());
    }

    if (json.getScreenHeight() != null) {
      Tilbakemelding.setScreenHeight(json.getScreenHeight());
    }

    if (json.getScreenWidth() != null) {
      Tilbakemelding.setScreenWidth(json.getScreenWidth());
    }

    if (json.getDocHeight() != null) {
      Tilbakemelding.setDocHeight(json.getDocHeight());
    }

    if (json.getDocWidth() != null) {
      Tilbakemelding.setDocWidth(json.getDocWidth());
    }

    if (json.getWinHeight() != null) {
      Tilbakemelding.setWinHeight(json.getWinHeight());
    }

    if (json.getWinWidth() != null) {
      Tilbakemelding.setWinWidth(json.getWinWidth());
    }

    if (json.getScrollX() != null) {
      Tilbakemelding.setScrollX(json.getScrollX());
    }

    if (json.getScrollY() != null) {
      Tilbakemelding.setScrollY(json.getScrollY());
    }

    if (json.getUserSatisfied() != null) {
      Tilbakemelding.setUserSatisfied(json.getUserSatisfied());
    }

    if (json.getHandledByAdmin() != null) {
      Tilbakemelding.setHandledByAdmin(json.getHandledByAdmin());
    }

    if (json.getAdminComment() != null) {
      Tilbakemelding.setAdminComment(json.getAdminComment());
    }

    return Tilbakemelding;
  }

  //Data to front-end
  @Override
  public TilbakemeldingJSON toJSON(Tilbakemelding Tilbakemelding, TilbakemeldingJSON json, Set<String> expandPaths,
                                   String currentPath) {
    super.toJSON(Tilbakemelding, json, expandPaths, currentPath);

    json.setMessageFromUser((Tilbakemelding.getMessageFromUser()));
    json.setPath((Tilbakemelding.getPath()));
    json.setReferer((Tilbakemelding.getReferer()));
    json.setUserAgent((Tilbakemelding.getUserAgent()));
    json.setScreenHeight((Tilbakemelding.getScreenHeight()));
    json.setScreenWidth((Tilbakemelding.getScreenWidth()));
    json.setDocHeight((Tilbakemelding.getDocHeight()));
    json.setDocWidth((Tilbakemelding.getDocWidth()));
    json.setWinHeight((Tilbakemelding.getWinHeight()));
    json.setWinWidth((Tilbakemelding.getWinWidth()));
    json.setScrollX((Tilbakemelding.getScrollX()));
    json.setScrollY((Tilbakemelding.getScrollY()));
    json.setUserSatisfied((Tilbakemelding.isUserSatisfied()));
    json.setHandledByAdmin((Tilbakemelding.isHandledByAdmin()));
    json.setAdminComment((Tilbakemelding.getAdminComment()));

    return json;
  }

  /**
   * Delete an Tilbakemelding
   * 
   * @param id
   * @return
   */
  @Transactional
  public TilbakemeldingJSON delete(String id) {
    Tilbakemelding Tilbakemelding = repository.findById(id);
    return delete(Tilbakemelding);
  }

  /**
   * Delete an Tilbakemelding
   * 
   * @param Tilbakemelding
   * @return
   */
  @Transactional
  public TilbakemeldingJSON delete(Tilbakemelding Tilbakemelding) {
    TilbakemeldingJSON tilbakemeldingJSON = toJSON(Tilbakemelding);
    tilbakemeldingJSON.setDeleted(true);
    repository.delete(Tilbakemelding);

    return tilbakemeldingJSON;
  }


}
