package no.einnsyn.apiv3.entities.lagretsoek;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoek;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekDTO;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekHit;
import no.einnsyn.apiv3.entities.lagretsoek.models.LagretSoekListQueryDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import no.einnsyn.apiv3.error.exceptions.NotFoundException;
import no.einnsyn.apiv3.utils.MailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LagretSoekService extends BaseService<LagretSoek, LagretSoekDTO> {

  @Getter private final LagretSoekRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  LagretSoekService proxy;

  @Value("${application.email.from}")
  private String emailFrom;

  @Value("${application.lagretSoek.maxResults:10}")
  private int maxResults = 10;

  private MailSender mailSender;

  public LagretSoekService(LagretSoekRepository repository, MailSender mailSender) {
    this.repository = repository;
    this.mailSender = mailSender;
  }

  public LagretSoek newObject() {
    return new LagretSoek();
  }

  public LagretSoekDTO newDTO() {
    return new LagretSoekDTO();
  }

  @Override
  protected LagretSoek fromDTO(LagretSoekDTO dto, LagretSoek lagretSoek) throws EInnsynException {
    super.fromDTO(dto, lagretSoek);

    if (dto.getBruker() != null) {
      var bruker = brukerService.returnExistingOrThrow(dto.getBruker());
      lagretSoek.setBruker(bruker);
    }

    if (dto.getLabel() != null) {
      lagretSoek.setLabel(dto.getLabel());
    }

    if (dto.getLegacyQuery() != null) {
      lagretSoek.setLegacyQuery(dto.getLegacyQuery());
    }

    if (dto.getSubscribe() != null) {
      lagretSoek.setSubscribe(dto.getSubscribe());
    }

    // TODO: Handle subscriptions (percolator)
    if (lagretSoek.getLegacyQueryEs() == null) {
      lagretSoek.setLegacyQueryEs("{}");
    }

    return lagretSoek;
  }

  @Override
  protected LagretSoekDTO toDTO(
      LagretSoek lagretSoek, LagretSoekDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(lagretSoek, dto, expandPaths, currentPath);

    dto.setBruker(
        brukerService.maybeExpand(lagretSoek.getBruker(), "bruker", expandPaths, currentPath));
    dto.setLabel(lagretSoek.getLabel());
    dto.setLegacyQuery(lagretSoek.getLegacyQuery());
    dto.setSubscribe(lagretSoek.isSubscribe());

    return dto;
  }

  @Override
  protected void deleteEntity(LagretSoek object) throws EInnsynException {
    // TODO: Handle subscriptions (percolator)
    super.deleteEntity(object);
  }

  /**
   * Add hit to lagret soek. Up to {maxHits} hits will be temporarily saved as LagretSoekHit, and
   * deleted after the user has been notified.
   *
   * @param document
   * @param legacyId
   */
  @Transactional
  public void addHit(BaseES document, UUID legacyId) {
    var documentId = document.getId();
    var hitCount = repository.addHitByLegacyId(legacyId);

    if (hitCount == null) {
      log.warn("Failed to add hit to LagretSoek {}", legacyId);
      return;
    }

    log.debug(
        "Matched document {} with percolator query {}. Search has {} hits.",
        document.getId(),
        legacyId,
        hitCount);

    // Cache hit for email notification
    if (hitCount <= 10) {
      var lagretSoek = repository.findByLegacyId(legacyId);
      var lagretSoekHit = new LagretSoekHit();
      var type = document.getType().getFirst();
      switch (type) {
        case "Saksmappe":
          var saksmappe = saksmappeService.findById(documentId);
          lagretSoekHit.setSaksmappe(saksmappe);
          break;
        case "Journalpost":
          var journalpost = journalpostService.findById(documentId);
          lagretSoekHit.setJournalpost(journalpost);
          break;
        case "Moetemappe":
          var moetemappe = moetemappeService.findById(documentId);
          lagretSoekHit.setMoetemappe(moetemappe);
          break;
        case "Moetesak", "Møtesaksregistrering": // Legacy
          var moetesak = moetesakService.findById(documentId);
          lagretSoekHit.setMoetesak(moetesak);
          break;
        default:
          // Couldn't determine document type
          return;
      }
      lagretSoekHit.setLagretSoek(lagretSoek);
      lagretSoek.addHit(lagretSoekHit);
    }
  }

  /**
   * Notify bruker about lagret soek hits.
   *
   * @param brukerId
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void notifyLagretSoek(String brukerId) {

    var bruker = brukerService.findById(brukerId);
    var lagretSoekList = repository.findLagretSoekWithHitsByBruker(brukerId).toList();

    // Build mail template context
    var context = new HashMap<String, Object>();
    context.put("bruker", bruker);
    context.put("maxHits", maxResults);
    context.put("lagretSoek", lagretSoekList.stream().map(this::getLagretSoekContext).toList());

    var emailTo = bruker.getEmail();
    var language = bruker.getLanguage();

    log.info("Sending LagretSoek hits to {}", emailTo);
    try {
      mailSender.send(emailFrom, emailTo, "lagretSoekSubscription", language, context);
    } catch (Exception e) {
      log.error("Failed to send LagretSoek hits to {}", emailTo, e);
      return;
    }

    var lagretSoekIds = lagretSoekList.stream().map(LagretSoek::getId).toList();
    repository.resetHitCount(lagretSoekIds);
    repository.deleteHits(lagretSoekIds);
  }

  /** Generate template context for a LagretSoek */
  Map<String, Object> getLagretSoekContext(LagretSoek lagretSoek) {
    var lagretSoekMap = new HashMap<String, Object>();
    lagretSoekMap.put("label", lagretSoek.getLabel());
    lagretSoekMap.put("hitCount", lagretSoek.getHitCount());
    lagretSoekMap.put("hasMoreHits", lagretSoek.getHitCount() > maxResults);
    lagretSoekMap.put("filterId", lagretSoek.getLegacyQuery());
    lagretSoekMap.put(
        "hitList", lagretSoek.getHitList().stream().map(this::getHitContext).toList());
    return lagretSoekMap;
  }

  /** Generate template context for LagretSoekHit */
  Map<String, Object> getHitContext(LagretSoekHit hit) {
    var hitMap = new HashMap<String, Object>();
    hitMap.put("saksmappe", getSaksmappeContext(hit.getSaksmappe()));
    hitMap.put("journalpost", getJournalpostContext(hit.getJournalpost()));
    hitMap.put("moetemappe", getMoetemappeContext(hit.getMoetemappe()));
    hitMap.put("moetesak", getMoetesakContext(hit.getMoetesak()));
    return hitMap;
  }

  /** Generate template context for Saksmappe */
  Map<String, Object> getSaksmappeContext(Saksmappe saksmappe) {
    if (saksmappe == null) {
      return null;
    }
    var saksmappeMap = new HashMap<String, Object>();
    saksmappeMap.put("offentligTittel", truncate(saksmappe.getOffentligTittel()));
    saksmappeMap.put("id", saksmappe.getId());
    saksmappeMap.put("iri", saksmappe.getSaksmappeIri());
    return saksmappeMap;
  }

  /** Generate template context for Journalpost */
  Map<String, Object> getJournalpostContext(Journalpost journalpost) {
    if (journalpost == null) {
      return null;
    }
    var journalpostMap = new HashMap<String, Object>();
    journalpostMap.put("offentligTittel", truncate(journalpost.getOffentligTittel()));
    journalpostMap.put("id", journalpost.getId());
    journalpostMap.put("iri", journalpost.getJournalpostIri());
    journalpostMap.put("saksmappe", getSaksmappeContext(journalpost.getSaksmappe()));
    return journalpostMap;
  }

  /** Generate template context for Moetemappe */
  Map<String, Object> getMoetemappeContext(Moetemappe moetemappe) {
    if (moetemappe == null) {
      return null;
    }
    var moetemappeMap = new HashMap<String, Object>();
    moetemappeMap.put("offentligTittel", truncate(moetemappe.getOffentligTittel()));
    moetemappeMap.put("id", moetemappe.getId());
    moetemappeMap.put("iri", moetemappe.getMoetemappeIri());
    return moetemappeMap;
  }

  /** Generate template context for Moetesak */
  Map<String, Object> getMoetesakContext(Moetesak moetesak) {
    if (moetesak == null) {
      return null;
    }
    var moetesakMap = new HashMap<String, Object>();
    moetesakMap.put("offentligTittel", truncate(moetesak.getOffentligTittel()));
    moetesakMap.put("id", moetesak.getId());
    moetesakMap.put("iri", moetesak.getMoetesakIri());
    return moetesakMap;
  }

  String truncate(String str) {
    if (str == null) {
      return null;
    }
    var maxLength = 60;
    return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
  }

  @Override
  protected Paginators<LagretSoek> getPaginators(BaseListQueryDTO params) {
    if (params instanceof LagretSoekListQueryDTO p && p.getBrukerId() != null) {
      var bruker = brukerService.findById(p.getBrukerId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(bruker, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(bruker, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * Authorize the list operation. Only users and admin can access their own LagretSoek.
   *
   * @param params The LagretSoek list query
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeList(BaseListQueryDTO params) throws EInnsynException {
    if (params instanceof LagretSoekListQueryDTO p
        && p.getBrukerId() != null
        && authenticationService.isSelf(p.getBrukerId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to list LagretSoek");
  }

  /**
   * Authorize the get operation. Admins and users with access to the given bruker can get
   * LagretSoek objects.
   *
   * @param id The LagretSoek ID
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeGet(String id) throws EInnsynException {
    var lagretSoek = proxy.findById(id);
    if (lagretSoek == null) {
      throw new NotFoundException("LagretSoek not found: " + id);
    }

    var lagretSoekBruker = lagretSoek.getBruker();
    if (lagretSoekBruker != null && authenticationService.isSelf(lagretSoekBruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to get " + id);
  }

  /**
   * Authorize the add operation. Users can add LagretSoek objects for themselves.
   *
   * @param dto The LagretSoek DTO
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(LagretSoekDTO dto) throws EInnsynException {
    if (authenticationService.isSelf(dto.getBruker().getId())) {
      return;
    }
    throw new ForbiddenException("Not authorized to add LagretSoek");
  }

  /**
   * Authorize the update operation. Only admin and owner can update LagretSoek objects.
   *
   * @param id The LagretSoek ID
   * @param dto The LagretSoek DTO
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, LagretSoekDTO dto) throws EInnsynException {
    var lagretSoek = proxy.findById(id);

    var bruker = lagretSoek.getBruker();
    if (bruker != null && authenticationService.isSelf(bruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to update " + id);
  }

  /**
   * Authorize the delete operation. Only users representing a bruker that owns the object can
   * delete.
   *
   * @param id The LagretSoek ID
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeDelete(String id) throws EInnsynException {
    if (authenticationService.isAdmin()) {
      return;
    }

    var lagretSoek = proxy.findById(id);
    var bruker = lagretSoek.getBruker();
    if (bruker != null && authenticationService.isSelf(bruker.getId())) {
      return;
    }

    throw new ForbiddenException("Not authorized to delete " + id);
  }
}
