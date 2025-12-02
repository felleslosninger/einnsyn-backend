package no.einnsyn.backend.entities.lagretsak;

import java.util.HashMap;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.exceptions.models.NotFoundException;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.bruker.models.ListByBrukerParameters;
import no.einnsyn.backend.entities.lagretsak.models.LagretSak;
import no.einnsyn.backend.entities.lagretsak.models.LagretSakDTO;
import no.einnsyn.backend.utils.mail.MailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LagretSakService extends BaseService<LagretSak, LagretSakDTO> {

  @Getter private final LagretSakRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  LagretSakService proxy;

  @Value("${application.email.from}")
  private String emailFrom;

  private MailSenderService mailSender;

  public LagretSakService(LagretSakRepository repository, MailSenderService mailSender) {
    this.repository = repository;
    this.mailSender = mailSender;
  }

  public LagretSak newObject() {
    return new LagretSak();
  }

  public LagretSakDTO newDTO() {
    return new LagretSakDTO();
  }

  /** LagretSak are unique by bruker + saksmappe / moetemappe */
  @Transactional(readOnly = true)
  @Override
  public Pair<String, LagretSak> findPropertyAndObjectByDTO(BaseDTO dto) {
    if (dto instanceof LagretSakDTO lagretSakDTO) {
      var brukerId = lagretSakDTO.getBruker().getId();
      var saksmappeField = lagretSakDTO.getSaksmappe();
      var saksmappeId = saksmappeField != null ? saksmappeField.getId() : null;
      var moetemappeField = lagretSakDTO.getMoetemappe();
      var moetemappeId = moetemappeField != null ? moetemappeField.getId() : null;

      if (saksmappeId != null) {
        var lagretSak = repository.findByBrukerAndSaksmappe(brukerId, saksmappeId);
        if (lagretSak != null) {
          return Pair.of("[brukerId, saksmappe]", lagretSak);
        }
      }

      if (moetemappeId != null) {
        var lagretSak = repository.findByBrukerAndMoetemappe(brukerId, moetemappeId);
        if (lagretSak != null) {
          return Pair.of("[brukerId, moetemappeId]", lagretSak);
        }
      }
    }

    return super.findPropertyAndObjectByDTO(dto);
  }

  @Override
  protected LagretSak fromDTO(LagretSakDTO dto, LagretSak lagretSak) throws EInnsynException {
    super.fromDTO(dto, lagretSak);

    if (dto.getBruker() != null) {
      var bruker = brukerService.returnExistingOrThrow(dto.getBruker());
      lagretSak.setBruker(bruker);
    }

    if (dto.getSaksmappe() != null) {
      var saksmappe = saksmappeService.returnExistingOrThrow(dto.getSaksmappe());
      lagretSak.setSaksmappe(saksmappe);
    }

    if (dto.getMoetemappe() != null) {
      var moetemappe = moetemappeService.returnExistingOrThrow(dto.getMoetemappe());
      lagretSak.setMoetemappe(moetemappe);
    }

    if (dto.getSubscribe() != null) {
      lagretSak.setSubscribe(dto.getSubscribe());
    }

    return lagretSak;
  }

  @Override
  protected LagretSakDTO toDTO(
      LagretSak lagretSak, LagretSakDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(lagretSak, dto, expandPaths, currentPath);

    dto.setBruker(
        brukerService.maybeExpand(lagretSak.getBruker(), "bruker", expandPaths, currentPath));
    dto.setSaksmappe(
        saksmappeService.maybeExpand(
            lagretSak.getSaksmappe(), "saksmappe", expandPaths, currentPath));
    dto.setMoetemappe(
        moetemappeService.maybeExpand(
            lagretSak.getMoetemappe(), "moetemappe", expandPaths, currentPath));
    dto.setSubscribe(lagretSak.isSubscribe());

    return dto;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void notifyLagretSak(String lagretSakId) {
    var lagretSak = proxy.findById(lagretSakId);
    var bruker = lagretSak.getBruker();
    var saksmappe = lagretSak.getSaksmappe();
    var moetemappe = lagretSak.getMoetemappe();
    var enhet = lagretSak.getEnhet();

    var context = new HashMap<String, Object>();
    context.put("bruker", bruker);
    context.put("lagretsak", lagretSak);
    context.put("saksmappe", saksmappe);
    context.put("moetemappe", moetemappe);
    context.put("enhet", enhet);

    var title =
        saksmappe != null ? saksmappe.getOffentligTittel() : moetemappe.getOffentligTittel();
    context.put("title", title);

    var iri = saksmappe != null ? saksmappe.getSaksmappeIri() : moetemappe.getMoetemappeIri();
    context.put("iri", iri);

    var templateName = saksmappe != null ? "lagretSakSubscription" : "lagretMoeteSubscription";

    var emailTo = bruker.getEmail();
    var language = bruker.getLanguage();

    log.info("Sending LagretSak hit to {}", emailTo);
    try {
      mailSender.send(emailFrom, emailTo, templateName, language, context);
    } catch (Exception e) {
      log.error("Failed to send LagretSak hits to {}", emailTo, e);
      return;
    }

    repository.resetHits(lagretSakId);
  }

  @Override
  protected Paginators<LagretSak> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListByBrukerParameters p && p.getBrukerId() != null) {
      var bruker = brukerService.findByIdOrThrow(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * Authorize the list operation. Only users and admin can access their own LagretSak.
   *
   * @param params The LagretSak list query
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeList(ListParameters params) throws EInnsynException {
    if (params instanceof ListByBrukerParameters p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to list LagretSak");
  }

  /**
   * Authorize the get operation. Admins and users with access to the given bruker can get LagretSak
   * objects.
   *
   * @param id The LagretSak ID
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    var lagretSak = proxy.findByIdOrThrow(id);
    if (lagretSak == null) {
      throw new NotFoundException("LagretSak not found: " + id);
    }

    var lagretSakBruker = lagretSak.getBruker();
    if (lagretSakBruker != null && authenticationService.isSelf(lagretSakBruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. Users can add LagretSak objects for themselves.
   *
   * @param dto The LagretSak DTO
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(LagretSakDTO dto) throws EInnsynException {
    if (authenticationService.isSelf(dto.getBruker().getId())) {
      return;
    }
    throw new AuthorizationException("Not authorized to add LagretSak");
  }

  /**
   * Authorize the update operation. Only admin and owner can update LagretSak objects.
   *
   * @param id The LagretSak ID
   * @param dto The LagretSak DTO
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, LagretSakDTO dto) throws EInnsynException {
    var lagretSak = proxy.findByIdOrThrow(id);

    var bruker = lagretSak.getBruker();
    if (bruker != null && authenticationService.isSelf(bruker.getId())) {
      return;
    }

    throw new AuthorizationException("Not authorized to update " + id);
  }

  /**
   * Authorize the delete operation. Only users representing a bruker that owns the object can
   * delete.
   *
   * @param id The LagretSak ID
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var lagretSak = proxy.findByIdOrThrow(id);
    var bruker = lagretSak.getBruker();
    if (bruker != null && authenticationService.isSelf(bruker.getId())) {
      return;
    }

    // Owner of the saksmappe can delete
    var saksmappe = lagretSak.getSaksmappe();
    if (saksmappe != null) {
      try {
        saksmappeService.authorizeDelete(saksmappe.getId());
        return;
      } catch (AuthorizationException e) {
      }
    }

    // Owner of the moetemappe can delete
    var moetemappe = lagretSak.getMoetemappe();
    if (moetemappe != null) {
      try {
        moetemappeService.authorizeDelete(moetemappe.getId());
        return;
      } catch (AuthorizationException e) {
      }
    }

    throw new AuthorizationException("Not authorized to delete " + id);
  }
}
