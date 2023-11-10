package no.einnsyn.apiv3.entities.enhet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.enhet.models.EnhetJSON;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelRepository;
import no.einnsyn.apiv3.entities.innsynskravdel.InnsynskravDelService;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.apiv3.entities.saksmappe.SaksmappeService;

@Service
public class EnhetService extends EinnsynObjectService<Enhet, EnhetJSON> {

  @Getter
  private final EnhetRepository repository;

  private final InnsynskravDelRepository innsynskravDelRepository;
  private final JournalpostRepository journalpostRepository;
  private final SaksmappeRepository saksmappeRepository;

  @Lazy
  @Resource
  private JournalpostService journalpostService;

  @Lazy
  @Resource
  private SaksmappeService saksmappeService;

  @Lazy
  @Resource
  private InnsynskravDelService innsynskravDelService;

  @Getter
  private EnhetService service = this;

  EnhetService(EnhetRepository repository, InnsynskravDelRepository innsynskravDelRepository,
      JournalpostRepository journalpostRepository, SaksmappeRepository saksmappeRepository) {
    this.repository = repository;
    this.innsynskravDelRepository = innsynskravDelRepository;
    this.journalpostRepository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
  }

  public Enhet newObject() {
    return new Enhet();
  }

  public EnhetJSON newJSON() {
    return new EnhetJSON();
  }


  @Override
  public Enhet fromJSON(EnhetJSON json, Enhet enhet, Set<String> paths, String currentPath) {
    super.fromJSON(json, enhet, paths, currentPath);

    if (json.getNavn() != null) {
      enhet.setNavn(json.getNavn());
    }

    if (json.getNavnNynorsk() != null) {
      enhet.setNavnNynorsk(json.getNavnNynorsk());
    }

    if (json.getNavnEngelsk() != null) {
      enhet.setNavnEngelsk(json.getNavnEngelsk());
    }

    if (json.getNavnSami() != null) {
      enhet.setNavnSami(json.getNavnSami());
    }

    if (json.getAvsluttetDato() != null) {
      enhet.setAvsluttetDato(json.getAvsluttetDato());
    }

    if (json.getInnsynskravEpost() != null) {
      enhet.setInnsynskravEpost(json.getInnsynskravEpost());
    }

    if (json.getKontaktpunktAdresse() != null) {
      enhet.setKontaktpunktAdresse(json.getKontaktpunktAdresse());
    }

    if (json.getKontaktpunktEpost() != null) {
      enhet.setKontaktpunktEpost(json.getKontaktpunktEpost());
    }

    if (json.getKontaktpunktTelefon() != null) {
      enhet.setKontaktpunktTelefon(json.getKontaktpunktTelefon());
    }

    if (json.getOrgnummer() != null) {
      enhet.setOrgnummer(json.getOrgnummer());
    }

    if (json.getEnhetskode() != null) {
      enhet.setEnhetskode(json.getEnhetskode());
    }

    if (json.getEnhetstype() != null) {
      enhet.setEnhetstype(json.getEnhetstype());
    }

    if (json.getSkjult() != null) {
      enhet.setSkjult(json.getSkjult());
    }

    if (json.getEFormidling() != null) {
      enhet.setEFormidling(json.getEFormidling());
    }

    if (json.getVisToppnode() != null) {
      enhet.setVisToppnode(json.getVisToppnode());
    }

    if (json.getErTeknisk() != null) {
      enhet.setErTeknisk(json.getErTeknisk());
    }

    if (json.getSkalKonvertereId() != null) {
      enhet.setSkalKonvertereId(json.getSkalKonvertereId());
    }

    if (json.getSkalMottaKvittering() != null) {
      enhet.setSkalMottaKvittering(json.getSkalMottaKvittering());
    }

    if (json.getOrderXmlVersjon() != null) {
      enhet.setOrderXmlVersjon(json.getOrderXmlVersjon());
    }

    if (json.getParent() != null) {
      Enhet parent = repository.findById(json.getParent().getId());
      enhet.setParent(parent);
    }

    // Add underenhets
    List<ExpandableField<EnhetJSON>> underenhetFieldList = json.getUnderenhet();
    if (underenhetFieldList != null) {
      underenhetFieldList.forEach(underenhetField -> {
        Enhet underenhet = null;
        if (underenhetField.getId() != null) {
          underenhet = repository.findById(underenhetField.getId());
        } else {
          String underenhetPath =
              currentPath.equals("") ? "journalpost" : currentPath + ".journalpost";
          paths.add(underenhetPath);
          underenhet = fromJSON(underenhetField.getExpandedObject(), paths, underenhetPath);
        }
        enhet.addUnderenhet(underenhet);
      });
    }

    return enhet;
  }


  @Override
  public EnhetJSON toJSON(Enhet enhet, EnhetJSON json, Set<String> expandPaths,
      String currentPath) {
    super.toJSON(enhet, json, expandPaths, currentPath);

    json.setNavn(enhet.getNavn());
    json.setNavnNynorsk(enhet.getNavnNynorsk());
    json.setNavnEngelsk(enhet.getNavnEngelsk());
    json.setNavnSami(enhet.getNavnSami());
    json.setAvsluttetDato(enhet.getAvsluttetDato());
    json.setInnsynskravEpost(enhet.getInnsynskravEpost());
    json.setKontaktpunktAdresse(enhet.getKontaktpunktAdresse());
    json.setKontaktpunktEpost(enhet.getKontaktpunktEpost());
    json.setKontaktpunktTelefon(enhet.getKontaktpunktTelefon());
    json.setOrgnummer(enhet.getOrgnummer());
    json.setEnhetskode(enhet.getEnhetskode());
    json.setEnhetstype(enhet.getEnhetstype());
    json.setSkjult(enhet.isSkjult());
    json.setEFormidling(enhet.isEFormidling());
    json.setVisToppnode(enhet.isVisToppnode());
    json.setErTeknisk(enhet.isErTeknisk());
    json.setSkalKonvertereId(enhet.isSkalKonvertereId());
    json.setSkalMottaKvittering(enhet.isSkalMottaKvittering());
    json.setOrderXmlVersjon(enhet.getOrderXmlVersjon());

    Enhet parent = enhet.getParent();
    if (parent != null) {
      json.setParent(maybeExpand(parent, "parent", expandPaths, currentPath));
    }

    // Underenhets
    List<ExpandableField<EnhetJSON>> underenhetListJSON = new ArrayList<>();
    List<Enhet> underenhetList = enhet.getUnderenhet();
    if (underenhetList != null) {
      underenhetList.forEach(underenhet -> underenhetListJSON
          .add(maybeExpand(underenhet, "underenhet", expandPaths, currentPath)));
    }
    json.setUnderenhet(underenhetListJSON);

    return json;
  }



  /**
   * Search the subtree under `root` for an enhet with matching enhetskode. Searching breadth-first
   * to avoid unnecessary DB queries.
   * 
   * @param enhetskode
   * @param root
   * @return
   */
  public Enhet findByEnhetskode(String enhetskode, Enhet root) {

    // Empty string is not a valid enhetskode
    if (enhetskode == null || root == null || enhetskode.equals("")) {
      return null;
    }

    Integer checkElementCount = 0;
    Integer queryChildrenCount = 0;
    List<Enhet> queue = new ArrayList<>();
    Set<Enhet> visited = new HashSet<>();

    // Search for enhet with matching enhetskode, breadth-first to avoid unnecessary DB queries
    queue.add(root);
    while (checkElementCount < queue.size()) {
      Enhet enhet = queue.get(checkElementCount);
      checkElementCount++;

      // Avoid infinite loops
      if (visited.contains(enhet)) {
        continue;
      }
      visited.add(enhet);

      if (enhetskode.equals(enhet.getEnhetskode())) {
        return enhet;
      }

      // Add more children to queue when needed
      while (checkElementCount >= queue.size() && queryChildrenCount < queue.size()) {
        Enhet querier = queue.get(queryChildrenCount);
        queryChildrenCount++;
        List<Enhet> underenhet = querier.getUnderenhet();
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
  public List<Enhet> getTransitiveEnhets(Enhet enhet) {
    List<Enhet> transitiveList = new ArrayList<>();
    Set<Enhet> visited = new HashSet<>();
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
   * @param id
   * @return
   */
  @Transactional
  public EnhetJSON delete(String id) {
    Enhet enhet = repository.findById(id);
    return delete(enhet);
  }

  /**
   * Delete an Enhet and all its descendants
   * 
   * @param enhet
   * @return
   */
  @Transactional
  public EnhetJSON delete(Enhet enhet) {
    EnhetJSON enhetJSON = toJSON(enhet);
    enhetJSON.setDeleted(true);

    // Delete all underenhets
    List<Enhet> underenhetList = enhet.getUnderenhet();
    if (underenhetList != null) {
      underenhetList.forEach(this::delete);
    }

    // Delete all innsynskravDels
    var innsynskravDelList = innsynskravDelRepository.findByEnhet(enhet);
    if (innsynskravDelList != null) {
      innsynskravDelList.forEach(innsynskravDelService::delete);
    }

    // Delete all saksmappes by this enhet
    var saksmappeStream = saksmappeRepository.findByJournalenhet(enhet);
    if (saksmappeStream != null) {
      saksmappeStream.forEach(saksmappeService::delete);
    }

    // Delete all journalposts by this enhet
    var journalpostStream = journalpostRepository.findByAdministrativEnhetObjekt(enhet);
    if (journalpostStream != null) {
      journalpostStream.forEach(journalpostRepository::delete);
    }

    repository.deleteById(enhet.getLegacyId());

    return enhetJSON;
  }

}
