package no.einnsyn.apiv3.entities.korrespondansepart;

import java.util.Set;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.Getter;
import no.einnsyn.apiv3.entities.einnsynobject.EinnsynObjectService;
import no.einnsyn.apiv3.entities.expandablefield.ExpandableField;
import no.einnsyn.apiv3.entities.journalpost.JournalpostRepository;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.korrespondansepart.models.Korrespondansepart;
import no.einnsyn.apiv3.entities.korrespondansepart.models.KorrespondansepartJSON;

@Service
public class KorrespondansepartService
    extends EinnsynObjectService<Korrespondansepart, KorrespondansepartJSON> {

  @Getter
  private final KorrespondansepartRepository repository;

  private final JournalpostRepository journalpostRepository;

  public KorrespondansepartService(KorrespondansepartRepository repository,
      JournalpostRepository journalpostRepository) {
    this.repository = repository;
    this.journalpostRepository = journalpostRepository;
  }

  public Korrespondansepart newObject() {
    return new Korrespondansepart();
  }

  public KorrespondansepartJSON newJSON() {
    return new KorrespondansepartJSON();
  }


  /**
   * Convert a JSON object to a Korrespondansepart
   * 
   * @param json
   * @param korrespondansepart
   * @param paths A list of paths containing new objects that will be created from this update
   * @param currentPath The current path in the object tree
   * @return
   */
  public Korrespondansepart fromJSON(KorrespondansepartJSON json,
      Korrespondansepart korrespondansepart, Set<String> paths, String currentPath) {
    super.fromJSON(json, korrespondansepart, paths, currentPath);

    if (json.getKorrespondanseparttype() != null) {
      korrespondansepart.setKorrespondanseparttype(json.getKorrespondanseparttype());
    }

    if (json.getKorrespondansepartNavn() != null) {
      korrespondansepart.setKorrespondansepartNavn(json.getKorrespondansepartNavn());
    }

    if (json.getKorrespondansepartNavnSensitiv() != null) {
      korrespondansepart
          .setKorrespondansepartNavnSensitiv(json.getKorrespondansepartNavnSensitiv());
    }

    if (json.getAdministrativEnhet() != null) {
      korrespondansepart.setAdministrativEnhet(json.getAdministrativEnhet());
    }

    if (json.getSaksbehandler() != null) {
      korrespondansepart.setSaksbehandler(json.getSaksbehandler());
    }

    if (json.getEpostadresse() != null) {
      korrespondansepart.setEpostadresse(json.getEpostadresse());
    }

    if (json.getPostnummer() != null) {
      korrespondansepart.setPostnummer(json.getPostnummer());
    }

    if (json.getErBehandlingsansvarlig() != null) {
      korrespondansepart.setErBehandlingsansvarlig(json.getErBehandlingsansvarlig());
    }

    ExpandableField<JournalpostJSON> journalpostField = json.getJournalpost();
    if (journalpostField != null) {
      Journalpost journalpost = journalpostRepository.findById(journalpostField.getId());
      korrespondansepart.setJournalpost(journalpost);
    }

    return korrespondansepart;
  }


  /**
   * Convert a Korrespondansepart to a JSON object
   * 
   * @param korrespondansepart
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return
   */
  public KorrespondansepartJSON toJSON(Korrespondansepart korrespondansepart,
      KorrespondansepartJSON json, Set<String> expandPaths, String currentPath) {
    super.toJSON(korrespondansepart, json, expandPaths, currentPath);

    json.setKorrespondanseparttype(korrespondansepart.getKorrespondanseparttype());
    json.setKorrespondansepartNavn(korrespondansepart.getKorrespondansepartNavn());
    json.setKorrespondansepartNavnSensitiv(korrespondansepart.getKorrespondansepartNavnSensitiv());
    json.setAdministrativEnhet(korrespondansepart.getAdministrativEnhet());
    json.setSaksbehandler(korrespondansepart.getSaksbehandler());
    json.setEpostadresse(korrespondansepart.getEpostadresse());
    json.setPostnummer(korrespondansepart.getPostnummer());
    json.setErBehandlingsansvarlig(korrespondansepart.getErBehandlingsansvarlig());

    return json;
  }


  /**
   * Delete a Korrespondansepart
   * 
   * @param id
   * @return
   */
  @Transactional
  public KorrespondansepartJSON delete(String id) {
    // This ID should be verified in the controller, so it should always exist.
    Korrespondansepart korrpart = repository.findById(id);
    return delete(korrpart);
  }

  /**
   * Delete a Korrespondansepart
   * 
   * @param korrpart
   * @return
   */
  @Transactional
  public KorrespondansepartJSON delete(Korrespondansepart korrpart) {
    KorrespondansepartJSON korrpartJSON = toJSON(korrpart);
    korrpartJSON.setDeleted(true);

    // Delete saksmappe
    repository.delete(korrpart);

    return korrpartJSON;
  }

}
