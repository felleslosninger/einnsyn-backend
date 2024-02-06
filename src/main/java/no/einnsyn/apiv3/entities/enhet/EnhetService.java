package no.einnsyn.apiv3.entities.enhet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.paginators.Paginators;
import no.einnsyn.apiv3.common.resultlist.ResultList;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetListQueryDTO;
import no.einnsyn.apiv3.entities.enhet.models.EnhetstypeEnum;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnhetService extends BaseService<Enhet, EnhetDTO> {

  @Getter private final EnhetRepository repository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private EnhetService proxy;

  private final InnsynskravDelRepository innsynskravDelRepository;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeRepository saksmappeRepository;

  EnhetService(
      EnhetRepository repository,
      InnsynskravDelRepository innsynskravDelRepository,
      JournalpostRepository journalpostRepository,
      SaksmappeRepository saksmappeRepository) {
    this.repository = repository;
    this.innsynskravDelRepository = innsynskravDelRepository;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
  }

  public Enhet newObject() {
    return new Enhet();
  }

  public EnhetDTO newDTO() {
    return new EnhetDTO();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public Enhet fromDTO(EnhetDTO dto, Enhet enhet, Set<String> paths, String currentPath)
      throws EInnsynException {
    super.fromDTO(dto, enhet, paths, currentPath);

    if (dto.getNavn() != null) {
      enhet.setNavn(dto.getNavn());
    }

    if (dto.getNavnNynorsk() != null) {
      enhet.setNavnNynorsk(dto.getNavnNynorsk());
    }

    if (dto.getNavnEngelsk() != null) {
      enhet.setNavnEngelsk(dto.getNavnEngelsk());
    }

    if (dto.getNavnSami() != null) {
      enhet.setNavnSami(dto.getNavnSami());
    }

    if (dto.getAvsluttetDato() != null) {
      enhet.setAvsluttetDato(LocalDate.parse(dto.getAvsluttetDato()));
    }

    if (dto.getInnsynskravEpost() != null) {
      enhet.setInnsynskravEpost(dto.getInnsynskravEpost());
    }

    if (dto.getKontaktpunktAdresse() != null) {
      enhet.setKontaktpunktAdresse(dto.getKontaktpunktAdresse());
    }

    if (dto.getKontaktpunktEpost() != null) {
      enhet.setKontaktpunktEpost(dto.getKontaktpunktEpost());
    }

    if (dto.getKontaktpunktTelefon() != null) {
      enhet.setKontaktpunktTelefon(dto.getKontaktpunktTelefon());
    }

    if (dto.getOrgnummer() != null) {
      enhet.setOrgnummer(dto.getOrgnummer());
    }

    if (dto.getEnhetskode() != null) {
      enhet.setEnhetskode(dto.getEnhetskode());
    }

    if (dto.getEnhetstype() != null) {
      enhet.setEnhetstype(EnhetstypeEnum.fromValue(dto.getEnhetstype()));
    }

    if (dto.getSkjult() != null) {
      enhet.setSkjult(dto.getSkjult());
    }

    if (dto.getEFormidling() != null) {
      enhet.setEFormidling(dto.getEFormidling());
    }

    if (dto.getVisToppnode() != null) {
      enhet.setVisToppnode(dto.getVisToppnode());
    }

    if (dto.getTeknisk() != null) {
      enhet.setErTeknisk(dto.getTeknisk());
    }

    if (dto.getSkalKonvertereId() != null) {
      enhet.setSkalKonvertereId(dto.getSkalKonvertereId());
    }

    if (dto.getSkalMottaKvittering() != null) {
      enhet.setSkalMottaKvittering(dto.getSkalMottaKvittering());
    }

    if (dto.getOrderXmlVersjon() != null) {
      enhet.setOrderXmlVersjon(dto.getOrderXmlVersjon());
    }

    if (dto.getParent() != null) {
      var parent = repository.findById(dto.getParent().getId()).orElse(null);
      enhet.setParent(parent);
    }

    // Persist before adding relations
    if (enhet.getId() == null) {
      enhet = repository.saveAndFlush(enhet);
    }

    // Add underenhets
    var underenhetFieldList = dto.getUnderenhet();
    if (underenhetFieldList != null) {
      for (var underenhetField : underenhetFieldList) {
        Enhet underenhet = null;
        if (underenhetField.getId() != null) {
          underenhet = repository.findById(underenhetField.getId()).orElse(null);
          if (underenhet == null) {
            throw new EInnsynException(
                "Underenhet with id " + underenhetField.getId() + " not found");
          }
        } else {
          var underenhetPath = currentPath.isEmpty() ? "underenhet" : currentPath + ".underenhet";
          var underenhetDTO = underenhetField.getExpandedObject();
          paths.add(underenhetPath);
          underenhet = proxy.fromDTO(underenhetDTO, paths, underenhetPath);
          if (underenhet == null) {
            throw new EInnsynException("Could not create underenhet from DTO");
          }
        }
        enhet.addUnderenhet(underenhet);
      }
    }

    return enhet;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public EnhetDTO toDTO(Enhet enhet, EnhetDTO dto, Set<String> expandPaths, String currentPath) {
    super.toDTO(enhet, dto, expandPaths, currentPath);

    dto.setNavn(enhet.getNavn());
    dto.setNavnNynorsk(enhet.getNavnNynorsk());
    dto.setNavnEngelsk(enhet.getNavnEngelsk());
    dto.setNavnSami(enhet.getNavnSami());
    if (enhet.getAvsluttetDato() != null) {
      dto.setAvsluttetDato(enhet.getAvsluttetDato().toString());
    }
    dto.setInnsynskravEpost(enhet.getInnsynskravEpost());
    dto.setKontaktpunktAdresse(enhet.getKontaktpunktAdresse());
    dto.setKontaktpunktEpost(enhet.getKontaktpunktEpost());
    dto.setKontaktpunktTelefon(enhet.getKontaktpunktTelefon());
    dto.setOrgnummer(enhet.getOrgnummer());
    dto.setEnhetskode(enhet.getEnhetskode());
    dto.setEnhetstype(enhet.getEnhetstype().toString());
    dto.setSkjult(enhet.isSkjult());
    dto.setEFormidling(enhet.isEFormidling());
    dto.setVisToppnode(enhet.isVisToppnode());
    dto.setTeknisk(enhet.isErTeknisk());
    dto.setSkalKonvertereId(enhet.isSkalKonvertereId());
    dto.setSkalMottaKvittering(enhet.isSkalMottaKvittering());
    dto.setOrderXmlVersjon(enhet.getOrderXmlVersjon());

    var parent = enhet.getParent();
    if (parent != null) {
      dto.setParent(maybeExpand(parent, "parent", expandPaths, currentPath));
    }

    // Underenhets
    var underenhetListDTO = dto.getUnderenhet();
    if (underenhetListDTO == null) {
      underenhetListDTO = new ArrayList<>();
      dto.setUnderenhet(underenhetListDTO);
    }
    var underenhetList = enhet.getUnderenhet();
    if (underenhetList != null) {
      for (var underenhet : underenhetList) {
        underenhetListDTO.add(maybeExpand(underenhet, "underenhet", expandPaths, currentPath));
      }
    }

    return dto;
  }

  /**
   * Search the subtree under `root` for an enhet with matching enhetskode. Searching breadth-first
   * to avoid unnecessary DB queries.
   *
   * @param enhetskode
   * @param root
   * @return
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Enhet findByEnhetskode(String enhetskode, Enhet root) {

    // Empty string is not a valid enhetskode
    if (enhetskode == null || root == null || enhetskode.isEmpty()) {
      return null;
    }

    var checkElementCount = 0;
    var queryChildrenCount = 0;
    var queue = new ArrayList<Enhet>();
    var visited = new HashSet<Enhet>();

    // Search for enhet with matching enhetskode, breadth-first to avoid unnecessary DB queries
    queue.add(root);
    while (checkElementCount < queue.size()) {
      var enhet = queue.get(checkElementCount);
      checkElementCount++;

      // Avoid infinite loops
      if (visited.contains(enhet)) {
        continue;
      }
      visited.add(enhet);

      // Enhet.enhetskode can be a semicolon-separated list of enhetskoder. Check if "enhetskode"
      // equals one of them.
      if (enhet.getEnhetskode() != null) {
        var enhetskodeList = enhet.getEnhetskode().split(";");
        for (var checkEnhetskode : enhetskodeList) {
          if (checkEnhetskode.trim().equals(enhetskode)) {
            return enhet;
          }
        }
      }

      // Add more children to queue when needed
      while (checkElementCount >= queue.size() && queryChildrenCount < queue.size()) {
        var querier = queue.get(queryChildrenCount);
        queryChildrenCount++;
        var underenhet = querier.getUnderenhet();
        if (underenhet != null) {
          queue.addAll(underenhet);
        }
      }
    }

    return null;
  }

  /**
   * Get a "transitive" list of ancestors for an Enhet object.
   *
   * @param enhet
   * @return
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public List<Enhet> getTransitiveEnhets(Enhet enhet) {
    var transitiveList = new ArrayList<Enhet>();
    var visited = new HashSet<Enhet>();
    Enhet parent = enhet;
    while (parent != null && !visited.contains(parent)) {
      transitiveList.add(parent);
      visited.add(parent);
      parent = parent.getParent();
      if (parent != null) {
        String enhetstype = parent.getEnhetstype().toString();
        if (enhetstype.equals("DummyEnhet") || enhetstype.equals("AdministrativEnhet")) {
          break;
        }
      }
    }
    return transitiveList;
  }

  /**
   * Delete an Enhet and all its descendants
   *
   * @param enhet
   * @return
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public EnhetDTO delete(Enhet enhet) throws EInnsynException {
    var dto = proxy.toDTO(enhet);
    dto.setDeleted(true);

    // Delete all underenhets
    var underenhetList = enhet.getUnderenhet();
    if (underenhetList != null) {
      for (var underenhet : underenhetList) {
        enhetService.delete(underenhet);
      }
    }

    // Delete all innsynskravDels
    var ikDelPage = innsynskravDelRepository.findByEnhet(enhet, PageRequest.of(0, 100));
    while (ikDelPage.hasContent()) {
      for (var innsynskravDel : ikDelPage) {
        innsynskravDelService.delete(innsynskravDel);
      }
      ikDelPage = innsynskravDelRepository.findByEnhet(enhet, ikDelPage.nextPageable());
    }

    // Delete all saksmappes by this enhet
    var saksmappePage = saksmappeRepository.findByJournalenhet(enhet, PageRequest.of(0, 100));
    while (saksmappePage.hasContent()) {
      for (var saksmappe : saksmappePage) {
        saksmappeService.delete(saksmappe);
      }
      saksmappePage = saksmappeRepository.findByJournalenhet(enhet, saksmappePage.nextPageable());
    }

    // Delete all journalposts by this enhet
    var jpPage = journalpostRepository.findByJournalenhet(enhet, PageRequest.of(0, 100));
    while (jpPage.hasContent()) {
      for (var journalpost : jpPage) {
        journalpostService.delete(journalpost);
      }
      jpPage = journalpostRepository.findByJournalenhet(enhet, jpPage.nextPageable());
    }

    repository.delete(enhet);

    return dto;
  }

  /**
   * @param enhetId
   * @param query
   * @return
   */
  public ResultList<EnhetDTO> getUnderenhetList(String enhetId, EnhetListQueryDTO query) {
    query.setParent(enhetId);
    return enhetService.list(query);
  }

  /**
   * @param enhetId
   * @param dto
   */
  public EnhetDTO addUnderenhet(String enhetId, EnhetDTO dto) throws EInnsynException {
    dto.setParent(new ExpandableField<>(enhetId));
    return enhetService.add(dto);
  }

  /**
   * @param enhetId
   * @param subId
   * @return
   */
  public EnhetDTO deleteUnderenhet(String enhetId, String subId) throws EInnsynException {
    enhetService.delete(subId);
    var enhet = enhetService.findById(enhetId);
    return enhetService.toDTO(enhet);
  }

  @Override
  public Paginators<Enhet> getPaginators(BaseListQueryDTO params) {
    if (params instanceof EnhetListQueryDTO p && p.getParent() != null) {
      var parent = enhetService.findById(p.getParent());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(parent, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(parent, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }
}
