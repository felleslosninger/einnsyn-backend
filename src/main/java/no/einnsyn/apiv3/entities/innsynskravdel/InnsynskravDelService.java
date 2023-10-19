package no.einnsyn.apiv3.entities.innsynskravdel;

import java.util.Set;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.enhet.EnhetService;
import no.einnsyn.apiv3.entities.enhet.models.Enhet;
import no.einnsyn.apiv3.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.apiv3.entities.innsynskrav.models.Innsynskrav;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDel;
import no.einnsyn.apiv3.entities.innsynskravdel.models.InnsynskravDelJSON;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.JournalpostService;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;

@Service
public class InnsynskravDelService
    extends EinnsynObjectService<InnsynskravDel, InnsynskravDelJSON> {

  @Getter
  private final InnsynskravDelRepository repository;
  private final InnsynskravRepository innsynskravRepository;
  private final JournalpostRepository journalpostRepository;
  private final JournalpostService journalpostService;
  private final EnhetService enhetService;


  public InnsynskravDelService(InnsynskravDelRepository repository,
      InnsynskravRepository innsynskravRepository, JournalpostRepository journalpostRepository,
      JournalpostService journalpostService, EnhetService enhetService) {
    super();
    this.repository = repository;
    this.innsynskravRepository = innsynskravRepository;
    this.journalpostRepository = journalpostRepository;
    this.journalpostService = journalpostService;
    this.enhetService = enhetService;
  }


  public InnsynskravDel newObject() {
    return new InnsynskravDel();
  }

  public InnsynskravDelJSON newJSON() {
    return new InnsynskravDelJSON();
  }


  public InnsynskravDel fromJSON(InnsynskravDelJSON json, InnsynskravDel innsynskravDel,
      Set<String> paths, String currentPath) {
    super.fromJSON(json, innsynskravDel, paths, currentPath);

    // Set reference to innsynskrav
    if (json.getInnsynskrav() != null) {
      var innsynskrav = innsynskravRepository.findById(json.getInnsynskrav().getId());
      innsynskravDel.setInnsynskrav(innsynskrav);
    }

    // Set reference to journalpost
    if (json.getJournalpost() != null) {
      var journalpost = journalpostRepository.findById(json.getJournalpost().getId());
      innsynskravDel.setJournalpost(journalpost);
    }

    // If the object doesn't exist, set the Enhet from the Journalpost
    if (innsynskravDel.getId() == null && innsynskravDel.getEnhet() == null) {
      Journalpost journalpost = innsynskravDel.getJournalpost();
      innsynskravDel.setEnhet(journalpost.getJournalenhet());
    }

    return innsynskravDel;
  }


  public InnsynskravDelJSON toJSON(InnsynskravDel innsynskravDel, InnsynskravDelJSON json,
      Set<String> expandPaths, String currentPath) {
    json = super.toJSON(innsynskravDel, json, expandPaths, currentPath);

    // Journalpost
    Journalpost journalpost = innsynskravDel.getJournalpost();
    json.setJournalpost(
        journalpostService.maybeExpand(journalpost, "journalpost", expandPaths, currentPath));

    // Enhet
    Enhet enhet = innsynskravDel.getEnhet();
    json.setEnhet(enhetService.maybeExpand(enhet, "enhet", expandPaths, currentPath));

    return json;
  }


  @Transactional
  public InnsynskravDelJSON delete(String id) {
    return delete(repository.findById(id));
  }

  @Transactional
  public InnsynskravDelJSON delete(InnsynskravDel innsynskravDel) {
    InnsynskravDelJSON json = newJSON();
    Innsynskrav innsynskrav = innsynskravDel.getInnsynskrav();

    // Remove reference to this innsynskravDel from innsynskrav
    innsynskrav.getInnsynskravDel().remove(innsynskravDel);

    repository.deleteById(innsynskravDel.getLegacyId());

    json.setDeleted(true);
    return json;
  }
}
