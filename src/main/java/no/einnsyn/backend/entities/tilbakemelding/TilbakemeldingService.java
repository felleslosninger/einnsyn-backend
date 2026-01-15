package no.einnsyn.backend.entities.tilbakemelding;

import java.util.Set;
import lombok.Getter;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.tilbakemelding.models.Tilbakemelding;
import no.einnsyn.backend.entities.tilbakemelding.models.TilbakemeldingDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TilbakemeldingService extends BaseService<Tilbakemelding, TilbakemeldingDTO> {

  @Getter private final TilbakemeldingRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private TilbakemeldingService proxy;

  TilbakemeldingService(TilbakemeldingRepository repository) {
    this.repository = repository;
  }

  public Tilbakemelding newObject() {
    return new Tilbakemelding();
  }

  public TilbakemeldingDTO newDTO() {
    return new TilbakemeldingDTO();
  }

  // Data from front-end
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  protected Tilbakemelding fromDTO(TilbakemeldingDTO dto, Tilbakemelding tilbakemelding)
      throws EInnsynException {
    super.fromDTO(dto, tilbakemelding);

    if (dto.getMessageFromUser() != null) {
      tilbakemelding.setMessageFromUser(dto.getMessageFromUser());
    }

    if (dto.getPath() != null) {
      tilbakemelding.setPath(dto.getPath());
    }

    if (dto.getReferer() != null) {
      tilbakemelding.setReferer(dto.getReferer());
    }

    if (dto.getUserAgent() != null) {
      tilbakemelding.setUserAgent(dto.getUserAgent());
    }

    if (dto.getScreenHeight() != null) {
      tilbakemelding.setScreenHeight(dto.getScreenHeight());
    }

    if (dto.getScreenWidth() != null) {
      tilbakemelding.setScreenWidth(dto.getScreenWidth());
    }

    if (dto.getDocHeight() != null) {
      tilbakemelding.setDocHeight(dto.getDocHeight());
    }

    if (dto.getDocWidth() != null) {
      tilbakemelding.setDocWidth(dto.getDocWidth());
    }

    if (dto.getWinHeight() != null) {
      tilbakemelding.setWinHeight(dto.getWinHeight());
    }

    if (dto.getWinWidth() != null) {
      tilbakemelding.setWinWidth(dto.getWinWidth());
    }

    if (dto.getScrollX() != null) {
      tilbakemelding.setScrollX(dto.getScrollX());
    }

    if (dto.getScrollY() != null) {
      tilbakemelding.setScrollY(dto.getScrollY());
    }

    if (dto.getUserSatisfied() != null) {
      tilbakemelding.setUserSatisfied(dto.getUserSatisfied());
    }

    if (dto.getHandledByAdmin() != null) {
      tilbakemelding.setHandledByAdmin(dto.getHandledByAdmin());
    }

    if (dto.getAdminComment() != null) {
      tilbakemelding.setAdminComment(dto.getAdminComment());
    }

    return tilbakemelding;
  }

  // Data to front-end
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  protected TilbakemeldingDTO toDTO(
      Tilbakemelding tilbakemelding,
      TilbakemeldingDTO dto,
      Set<String> expandPaths,
      String currentPath) {
    super.toDTO(tilbakemelding, dto, expandPaths, currentPath);

    dto.setMessageFromUser((tilbakemelding.getMessageFromUser()));
    dto.setPath((tilbakemelding.getPath()));
    dto.setReferer((tilbakemelding.getReferer()));
    dto.setUserAgent((tilbakemelding.getUserAgent()));
    dto.setScreenHeight((tilbakemelding.getScreenHeight()));
    dto.setScreenWidth((tilbakemelding.getScreenWidth()));
    dto.setDocHeight((tilbakemelding.getDocHeight()));
    dto.setDocWidth((tilbakemelding.getDocWidth()));
    dto.setWinHeight((tilbakemelding.getWinHeight()));
    dto.setWinWidth((tilbakemelding.getWinWidth()));
    dto.setScrollX((tilbakemelding.getScrollX()));
    dto.setScrollY((tilbakemelding.getScrollY()));
    dto.setUserSatisfied((tilbakemelding.isUserSatisfied()));
    dto.setHandledByAdmin((tilbakemelding.isHandledByAdmin()));
    dto.setAdminComment((tilbakemelding.getAdminComment()));

    return dto;
  }

  /**
   * Only admin can list Tilbakemelding
   *
   * @throws AuthorizationException if not authorized
   */
  @Override
  protected void authorizeList(ListParameters params) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException("Not authorized to list Tilbakemelding");
    }
  }

  /**
   * Only admin can get Tilbakemelding
   *
   * @param id of Tilbakemelding
   * @throws AuthorizationException if not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException("Not authorized to get " + id);
    }
  }

  /**
   * Anyone can add Tilbakemelding
   *
   * @param dto representing Tilbakemelding
   * @throws AuthorizationException if not authorized
   */
  @Override
  protected void authorizeAdd(TilbakemeldingDTO dto) throws EInnsynException {
    // No authorization needed
  }

  /**
   * Only admin can update Tilbakemelding
   *
   * @param id of Tilbakemelding
   * @param dto representing Tilbakemelding
   * @throws AuthorizationException if not authorized
   */
  @Override
  protected void authorizeUpdate(String id, TilbakemeldingDTO dto) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException("Not authorized to update " + id);
    }
  }

  /**
   * Only admin can delete Tilbakemelding
   *
   * @param id of Tilbakemelding
   * @throws AuthorizationException if not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    if (!authenticationService.isAdmin()) {
      throw new AuthorizationException("Not authorized to delete " + id);
    }
  }
}
