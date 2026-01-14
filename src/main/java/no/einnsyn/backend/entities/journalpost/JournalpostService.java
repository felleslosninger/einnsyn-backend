package no.einnsyn.backend.entities.journalpost;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.paginators.Paginators;
import no.einnsyn.backend.common.queryparameters.models.GetParameters;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.common.responses.models.PaginatedList;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseDTO;
import no.einnsyn.backend.entities.dokumentbeskrivelse.models.DokumentbeskrivelseES;
import no.einnsyn.backend.entities.enhet.models.Enhet;
import no.einnsyn.backend.entities.innsynskrav.InnsynskravRepository;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.journalpost.models.JournalpostES;
import no.einnsyn.backend.entities.journalpost.models.JournalposttypeResolver;
import no.einnsyn.backend.entities.journalpost.models.ListByJournalpostParameters;
import no.einnsyn.backend.entities.korrespondansepart.models.*;
import no.einnsyn.backend.entities.registrering.RegistreringService;
import no.einnsyn.backend.entities.saksmappe.SaksmappeRepository;
import no.einnsyn.backend.entities.saksmappe.models.ListBySaksmappeParameters;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeES.SaksmappeWithoutChildrenES;
import no.einnsyn.backend.entities.skjerming.models.SkjermingES;
import no.einnsyn.backend.utils.ExpandPathResolver;
import no.einnsyn.backend.utils.TimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("java:S1192") // Allow multiple string literals
@Slf4j
public class JournalpostService extends RegistreringService<Journalpost, JournalpostDTO> {

  @Getter private final JournalpostRepository repository;

  private final SaksmappeRepository saksmappeRepository;

  @SuppressWarnings("java:S6813")
  @Getter
  @Lazy
  @Autowired
  private JournalpostService proxy;

  private final InnsynskravRepository innsynskravRepository;

  JournalpostService(
      JournalpostRepository journalpostRepository,
      SaksmappeRepository saksmappeRepository,
      InnsynskravRepository innsynskravRepository) {
    super();
    this.repository = journalpostRepository;
    this.saksmappeRepository = saksmappeRepository;
    this.innsynskravRepository = innsynskravRepository;
  }

  public Journalpost newObject() {
    return new Journalpost();
  }

  public JournalpostDTO newDTO() {
    return new JournalpostDTO();
  }

  /**
   * Override scheduleIndex to also reindex the parent saksmappe.
   *
   * @param journalpost
   * @param recurseDirection -1 for parents, 1 for children, 0 for both
   */
  @Override
  public boolean scheduleIndex(String journalpostId, int recurseDirection) {
    var isScheduled = super.scheduleIndex(journalpostId, recurseDirection);

    // Index saksmappe
    if (recurseDirection <= 0 && !isScheduled) {
      var saksmappeId = saksmappeRepository.findIdByJournalpostId(journalpostId);
      if (saksmappeId != null) {
        saksmappeService.scheduleIndex(saksmappeId, -1);
      }
    }

    return true;
  }

  /**
   * Create a Journalpost from a DTO object. This will recursively also create children elements, if
   * they are given in the DTO object.
   *
   * @param dto The DTO object
   * @param journalpost The Journalpost object
   * @return The Journalpost object
   */
  @Override
  protected Journalpost fromDTO(JournalpostDTO dto, Journalpost journalpost)
      throws EInnsynException {
    super.fromDTO(dto, journalpost);

    if (dto.getJournalaar() != null) {
      journalpost.setJournalaar(dto.getJournalaar());
    }

    if (dto.getJournalsekvensnummer() != null) {
      journalpost.setJournalsekvensnummer(dto.getJournalsekvensnummer());
    }

    if (dto.getJournalpostnummer() != null) {
      journalpost.setJournalpostnummer(dto.getJournalpostnummer());
    }

    if (dto.getJournalposttype() != null) {
      journalpost.setJournalposttype(dto.getJournalposttype());
    }

    if (dto.getLegacyJournalposttype() != null) {
      journalpost.setLegacyJournalposttype(dto.getLegacyJournalposttype());
      journalpost.setJournalposttype(
          JournalposttypeResolver.resolve(dto.getLegacyJournalposttype()).toString());
    }

    // TODO: Remove this when the old API isn't used anymore
    if (journalpost.getLegacyJournalposttype() == null) {
      journalpost.setLegacyJournalposttype(journalpost.getJournalposttype());
    }

    if (dto.getJournaldato() != null) {
      journalpost.setJournaldato(LocalDate.parse(dto.getJournaldato()));
    }

    if (dto.getDokumentetsDato() != null) {
      journalpost.setDokumentdato(LocalDate.parse(dto.getDokumentetsDato()));
    }

    // Update saksmappe
    var saksmappeField = dto.getSaksmappe();
    if (saksmappeField != null) {
      var saksmappe = saksmappeService.findByIdOrThrow(saksmappeField.getId());
      journalpost.setSaksmappe(saksmappe);
    }

    // If we don't have an ID, persist the object before adding relations
    if (journalpost.getId() == null) {
      journalpost = repository.saveAndFlush(journalpost);
    }

    // Update skjerming
    var skjermingField = dto.getSkjerming();
    if (skjermingField != null) {
      journalpost.setSkjerming(skjermingService.createOrReturnExisting(skjermingField));
    }

    // Set default administrativEnhet before korrespondanseparts are added (they might override)
    if (dto.getAdministrativEnhet() != null && dto.getAdministrativEnhetObjekt() != null) {
      journalpost.setAdministrativEnhet(dto.getAdministrativEnhet());
      var administrativEnhetObjekt =
          enhetService.returnExistingOrThrow(dto.getAdministrativEnhetObjekt());
      journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
    }
    // AdministrativEnhet code is given, look up the object
    else if (dto.getAdministrativEnhet() != null) {
      var administrativEnhetKode = dto.getAdministrativEnhet();
      var administrativEnhetObjekt =
          journalpostService.getAdministrativEnhetObjekt(journalpost, administrativEnhetKode);
      journalpost.setAdministrativEnhet(administrativEnhetKode);
      journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
    }
    // AdministrativEnhetObjekt is given, remove administrativEnhet and set administrativEnhetObjekt
    else if (dto.getAdministrativEnhetObjekt() != null) {
      var administrativEnhetObjekt =
          enhetService.returnExistingOrThrow(dto.getAdministrativEnhetObjekt());
      journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
    }

    // There is no administrativ enhet, use the one from Saksmappe
    if (journalpost.getAdministrativEnhetObjekt() == null) {
      var saksmappe = journalpost.getSaksmappe();
      if (saksmappe != null) {
        var administrativEnhetObjekt = saksmappe.getAdministrativEnhetObjekt();
        journalpost.setAdministrativEnhet(saksmappe.getAdministrativEnhet());
        journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
        journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
      }
    }

    // Couldn't find administrativ enhet from Saksmappe (unlikely), use journalenhet
    if (journalpost.getAdministrativEnhetObjekt() == null) {
      var administrativEnhetObjekt = journalpost.getJournalenhet();
      journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
      journalpost.setArkivskaper(administrativEnhetObjekt.getIri());
    }

    // Update korrespondansepart
    var korrpartFieldList = dto.getKorrespondansepart();
    if (korrpartFieldList != null) {
      for (var korrpartField : korrpartFieldList) {
        var korrespondansepart = korrespondansepartService.createOrReturnExisting(korrpartField);
        korrespondansepart.setParentJournalpost(journalpost);
        journalpost.addKorrespondansepart(korrespondansepart);
      }
    }

    updateAdmEnhetFromKorrPartList(journalpost);

    // Update dokumentbeskrivelse
    var dokbeskFieldList = dto.getDokumentbeskrivelse();
    if (dokbeskFieldList != null) {
      for (var dokbeskField : dokbeskFieldList) {
        journalpost.addDokumentbeskrivelse(
            dokumentbeskrivelseService.createOrReturnExisting(dokbeskField));
      }
    }

    // legacyFoelgsakenReferanse
    if (dto.getLegacyFoelgsakenReferanse() != null) {
      journalpost.setFoelgsakenReferanse(dto.getLegacyFoelgsakenReferanse());
    }

    var slugBase = getSlugBase(journalpost);
    journalpost = setSlug(journalpost, slugBase);

    return journalpost;
  }

  @Override
  public String getSlugBase(Journalpost journalpost) {
    var slugBase = "";
    if (journalpost.getSaksmappe() != null
        && journalpost.getSaksmappe().getSaksaar() != null
        && journalpost.getSaksmappe().getSakssekvensnummer() != null) {
      var saksaar = journalpost.getSaksmappe().getSaksaar();
      var sakssekvensnummer = journalpost.getSaksmappe().getSakssekvensnummer();
      slugBase += saksaar + "-" + sakssekvensnummer + "-";
    }
    if (journalpost.getJournalsekvensnummer() != null) {
      slugBase += journalpost.getJournalsekvensnummer() + "-";
    }
    return slugBase + journalpost.getOffentligTittel();
  }

  /**
   * Convert a Journalpost to a JSON object.
   *
   * @param journalpost The Journalpost object
   * @param dto The JournalpostDTO object
   * @param expandPaths A list of paths to expand
   * @param currentPath The current path in the object tree
   * @return The JournalpostDTO object
   */
  @Override
  protected JournalpostDTO toDTO(
      Journalpost journalpost, JournalpostDTO dto, Set<String> expandPaths, String currentPath) {

    super.toDTO(journalpost, dto, expandPaths, currentPath);

    dto.setJournalaar(journalpost.getJournalaar());
    dto.setJournalpostnummer(journalpost.getJournalpostnummer());
    dto.setJournalposttype(journalpost.getJournalposttype());
    dto.setLegacyJournalposttype(journalpost.getLegacyJournalposttype());

    if (journalpost.getJournaldato() != null) {
      dto.setJournaldato(journalpost.getJournaldato().toString());
    }
    if (journalpost.getDokumentdato() != null) {
      dto.setDokumentetsDato(journalpost.getDokumentdato().toString());
    }

    dto.setSaksmappe(
        saksmappeService.maybeExpand(
            journalpost.getSaksmappe(), "saksmappe", expandPaths, currentPath));

    dto.setAdministrativEnhet(journalpost.getAdministrativEnhet());

    // Only document owners can see Journalsekvensnummer
    if (getProxy().isOwnerOf(journalpost)) {
      dto.setJournalsekvensnummer(journalpost.getJournalsekvensnummer());
    }

    // Administrativ enhet
    dto.setAdministrativEnhetObjekt(
        enhetService.maybeExpand(
            journalpost.getAdministrativEnhetObjekt(),
            "administrativEnhetObjekt",
            expandPaths,
            currentPath));

    // Skjerming
    dto.setSkjerming(
        skjermingService.maybeExpand(
            journalpost.getSkjerming(), "skjerming", expandPaths, currentPath));

    // Korrespondansepart
    dto.setKorrespondansepart(
        korrespondansepartService.maybeExpand(
            journalpost.getKorrespondansepart(), "korrespondansepart", expandPaths, currentPath));

    // Dokumentbeskrivelse
    dto.setDokumentbeskrivelse(
        dokumentbeskrivelseService.maybeExpand(
            journalpost.getDokumentbeskrivelse(), "dokumentbeskrivelse", expandPaths, currentPath));

    // Legacy følgsakenReferanse
    dto.setLegacyFoelgsakenReferanse(journalpost.getFoelgsakenReferanse());

    return dto;
  }

  @Override
  public BaseES toLegacyES(Journalpost journalpost) {
    return toLegacyES(journalpost, new JournalpostES());
  }

  @Override
  public BaseES toLegacyES(Journalpost journalpost, BaseES es) {
    super.toLegacyES(journalpost, es);
    if (es instanceof JournalpostES journalpostES) {
      journalpostES.setJournalaar("" + journalpost.getJournalaar());
      journalpostES.setJournalsekvensnummer("" + journalpost.getJournalsekvensnummer());
      journalpostES.setJournalpostnummer("" + journalpost.getJournalpostnummer());
      journalpostES.setJournalposttype(journalpost.getLegacyJournalposttype());
      if (journalpost.getJournaldato() != null) {
        journalpostES.setJournaldato(journalpost.getJournaldato().toString());
      }
      if (journalpost.getDokumentdato() != null) {
        journalpostES.setDokumentetsDato(journalpost.getDokumentdato().toString());
      }

      // Parent saksmappe
      var parent = journalpost.getSaksmappe();
      if (parent != null) {
        var parentES =
            (SaksmappeWithoutChildrenES)
                saksmappeService.toLegacyES(parent, new SaksmappeWithoutChildrenES());
        journalpostES.setParent(parentES);
        // Add journalpostnummer to saksnummerGenerert
        var saksnummerGenerert =
            parentES.getSaksnummerGenerert().stream()
                .map(saksnummer -> saksnummer + "-" + journalpost.getJournalpostnummer())
                .toList();
        journalpostES.setSaksnummerGenerert(saksnummerGenerert);

        var saksaar = "" + parent.getSaksaar();
        var sakssekvensnummer = "" + parent.getSakssekvensnummer();
        journalpostES.setSaksaar(saksaar);
        journalpostES.setSakssekvensnummer(sakssekvensnummer);
        journalpostES.setSaksnummer(saksaar + "/" + sakssekvensnummer);
      }

      // Skjerming
      var skjerming = journalpost.getSkjerming();
      if (skjerming != null) {
        journalpostES.setSkjerming(
            (SkjermingES) skjermingService.toLegacyES(skjerming, new SkjermingES()));
      }

      // Korrespondanseparts
      var korrespondansepart = journalpost.getKorrespondansepart();
      if (korrespondansepart != null) {
        var korrespondansepartES =
            korrespondansepart.stream()
                .map(
                    k ->
                        (KorrespondansepartES)
                            korrespondansepartService.toLegacyES(k, new KorrespondansepartES()))
                .toList();
        journalpostES.setKorrespondansepart(korrespondansepartES);
      } else {
        journalpostES.setKorrespondansepart(List.of());
      }

      // Dokumentbeskrivelses
      journalpostES.setFulltext(false);
      var dokumentbeskrivelseList = journalpost.getDokumentbeskrivelse();

      if (dokumentbeskrivelseList != null) {
        var dokumentbeskrivelseES =
            dokumentbeskrivelseList.stream()
                .map(
                    dokumentbeskrivelse ->
                        (DokumentbeskrivelseES)
                            dokumentbeskrivelseService.toLegacyES(
                                dokumentbeskrivelse, new DokumentbeskrivelseES()))
                .toList();
        journalpostES.setDokumentbeskrivelse(dokumentbeskrivelseES);
        for (var dokument : dokumentbeskrivelseES) {
          // A dokumentobjekt must have a link to a fulltext file, so we can safely mark the
          // journalpost if at least one dokumentobjekt is present.
          if (dokument.getDokumentobjekt() != null && !dokument.getDokumentobjekt().isEmpty()) {
            journalpostES.setFulltext(true);
            break;
          }
        }
      } else {
        journalpostES.setDokumentbeskrivelse(List.of());
      }

      // StandardDato
      journalpostES.setStandardDato(
          TimeConverter.generateStandardDato(journalpost.getJournaldato()));

      // Sorteringstype
      journalpostES.setSorteringstype(journalpost.getJournalposttype());
    }
    return es;
  }

  /**
   * Delete a Journalpost
   *
   * @param journalpost The Journalpost object
   */
  @Override
  protected void deleteEntity(Journalpost journalpost) throws EInnsynException {
    // Delete all korrespondanseparts
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      journalpost.setKorrespondansepart(null);
      for (var korrespondansepart : korrespondansepartList) {
        korrespondansepartService.delete(korrespondansepart.getId());
      }
    }

    // Unrelate all dokumentbeskrivelses
    var dokbeskList = journalpost.getDokumentbeskrivelse();
    if (dokbeskList != null) {
      journalpost.setDokumentbeskrivelse(null);
      for (var dokbesk : dokbeskList) {
        dokumentbeskrivelseService.deleteIfOrphan(dokbesk);
      }
    }

    // Unrelate skjerming, delete if orphan
    var skjerming = journalpost.getSkjerming();
    if (skjerming != null) {
      journalpost.setSkjerming(null);
      skjermingService.deleteIfOrphan(skjerming);
    }

    // Remove journalpost from all innsynskrav
    try (var innsynskravStream = innsynskravRepository.streamByJournalpost(journalpost)) {
      var innsynskravIterator = innsynskravStream.iterator();
      while (innsynskravIterator.hasNext()) {
        var innsynskrav = innsynskravIterator.next();
        innsynskrav.setJournalpost(null);
      }
    }

    super.deleteEntity(journalpost);
  }

  /**
   * Get custom paginator functions that filters by saksmappeId
   *
   * @param params The query parameters
   */
  @Override
  protected Paginators<Journalpost> getPaginators(ListParameters params) throws EInnsynException {
    if (params instanceof ListBySaksmappeParameters p && p.getSaksmappeId() != null) {
      var saksmappe = saksmappeService.findByIdOrThrow(p.getSaksmappeId());
      return new Paginators<>(
          (pivot, pageRequest) -> repository.paginateAsc(saksmappe, pivot, pageRequest),
          (pivot, pageRequest) -> repository.paginateDesc(saksmappe, pivot, pageRequest));
    }
    return super.getPaginators(params);
  }

  /**
   * Get the administrativ enhet kode for a Journalpost. First, look for a korrespondansepart with
   * erBehandlingsansvarlig = true, then fall back to the saksmappe's administrativEnhet.
   *
   * @param journalpostId The journalpost ID
   * @return The administrativ enhet kode
   */
  @Transactional(readOnly = true)
  public String getAdministrativEnhetKode(String journalpostId) {
    var journalpost = journalpostService.findById(journalpostId);
    if (journalpost == null) {
      return null;
    }
    return getAdministrativEnhetKode(journalpost);
  }

  /**
   * Fetch the administrativ enhet from the korrespondansepart that has erBehandlingsansvarlig set
   *
   * @param journalpost
   */
  public void updateAdmEnhetFromKorrPartList(Journalpost journalpost) {
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList == null) {
      return;
    }
    for (var korrpart : korrespondansepartList) {
      if (korrpart.isErBehandlingsansvarlig() && korrpart.getAdministrativEnhet() != null) {
        var administrativEnhetKode = korrpart.getAdministrativEnhet();
        var administrativEnhetObjekt =
            journalpostService.getAdministrativEnhetObjekt(journalpost, administrativEnhetKode);
        journalpost.setAdministrativEnhet(administrativEnhetKode);
        journalpost.setAdministrativEnhetObjekt(administrativEnhetObjekt);
        return;
      }
    }
  }

  /**
   * Get the administrativ enhet kode for a Journalpost. First, look for a korrespondansepart with
   * erBehandlingsansvarlig = true, then fall back to the saksmappe's administrativEnhet.
   *
   * <p>Protected method that expects an open transaction.
   *
   * @param journalpost The journalpost
   * @return The administrativ enhet kode
   */
  protected String getAdministrativEnhetKode(Journalpost journalpost) {
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      for (var korrespondansepart : korrespondansepartList) {
        if (korrespondansepart.isErBehandlingsansvarlig()) {
          return korrespondansepart.getAdministrativEnhet();
        }
      }
    }
    return journalpost.getSaksmappe().getAdministrativEnhet();
  }

  /**
   * Get the administrativ enhet object for a Journalpost. Get the administrativEnhetKode, and look
   * up the Enhet object.
   *
   * @param journalpost The journalpost ID
   * @return The administrativ enhet object
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Enhet getAdministrativEnhetObjekt(Journalpost journalpost) {
    var enhetskode = getAdministrativEnhetKode(journalpost);
    return getProxy().getAdministrativEnhetObjekt(journalpost, enhetskode);
  }

  /**
   * Get the administrativ enhet object for a Journalpost. Get the administrativEnhetKode, and look
   * up the Enhet object.
   *
   * @param journalpost The journalpost ID
   * @param enhetskode The enhetskode
   * @return The administrativ enhet object
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public Enhet getAdministrativEnhetObjekt(Journalpost journalpost, String enhetskode) {
    var enhetObjekt = enhetService.findByEnhetskode(enhetskode, journalpost.getJournalenhet());
    if (enhetObjekt != null) {
      return enhetObjekt;
    }
    return journalpost.getSaksmappe().getAdministrativEnhetObjekt();
  }

  /**
   * Get the saksbehandler for a Journalpost. Look for a Korrespondansepart with
   * erBehandlingsansvarlig = true.
   *
   * @param journalpostId The journalpost ID
   * @return The saksbehandler
   */
  @Transactional(readOnly = true)
  public Korrespondansepart getSaksbehandlerKorrespondansepart(String journalpostId) {
    var journalpost = journalpostService.findById(journalpostId);
    if (journalpost == null) {
      return null;
    }
    return journalpostService.getSaksbehandlerKorrespondansepart(journalpost);
  }

  /**
   * Get the saksbehandler for a Journalpost. Look for a Korrespondansepart with
   * erBehandlingsansvarlig = true. If none is found, a legacy resolver is called.
   *
   * <p>Protected method that expects an open transaction.
   *
   * @param journalpost The journalpost
   * @return The saksbehandler
   */
  protected Korrespondansepart getSaksbehandlerKorrespondansepart(Journalpost journalpost) {
    var korrespondansepartList = journalpost.getKorrespondansepart();
    if (korrespondansepartList != null) {
      for (var korrespondansepart : korrespondansepartList) {
        if (korrespondansepart.isErBehandlingsansvarlig()) {
          return korrespondansepart;
        }
      }
      return resolveLegacySaksbehandler(journalpost);
    }
    return null;
  }

  /**
   * Uses the legacy method to identify the most likely Saksbehandler based on Journalposttype and
   * Korrespondanseparttype.
   */
  protected Korrespondansepart resolveLegacySaksbehandler(Journalpost journalpost) {
    var korrespondansepartsWithSaksbehandler =
        journalpost.getKorrespondansepart().stream()
            .filter(kp -> !kp.getKorrespondanseparttype().endsWith("kopimottaker"))
            .filter(kp -> korrespondansepartMatchesJournalpostDirection(journalpost, kp))
            .filter(kp -> kp.getSaksbehandler() != null)
            .filter(kp -> !kp.getSaksbehandler().trim().isEmpty())
            .filter(kp -> !kp.getSaksbehandler().toLowerCase().contains("ufordelt"))
            .toList();
    var korrespondansepartWithSaksbehandlerAndAdministrativEnhet =
        korrespondansepartsWithSaksbehandler.stream()
            .filter(kp -> kp.getAdministrativEnhet() != null)
            .filter(kp -> !kp.getAdministrativEnhet().trim().isEmpty())
            .filter(kp -> !kp.getAdministrativEnhet().toLowerCase().contains("ufordelt"))
            .min(this::sortRegularKorrespondansepartBeforeInternal)
            .orElse(null);

    if (korrespondansepartWithSaksbehandlerAndAdministrativEnhet != null) {
      return korrespondansepartWithSaksbehandlerAndAdministrativEnhet;
    }
    // Fallback to korrespondansepart with saksbehandler only.
    return korrespondansepartsWithSaksbehandler.stream()
        .min(this::sortRegularKorrespondansepartBeforeInternal)
        .orElse(null);
  }

  /**
   * Match a Korrespondansepart with the direction of a Journalpost to assist in resolving the
   * Saksbehandler.
   *
   * <p>- For incoming Journalpost the Korrespondansepart must be a kind of recipient - For outgoing
   * Journalpost the Korrespondansepart must be a kind of sender
   */
  private boolean korrespondansepartMatchesJournalpostDirection(
      Journalpost journalpost, Korrespondansepart korrespondansepart) {
    if (journalpost
        .getJournalposttype()
        .equals(JournalpostDTO.JournalposttypeEnum.INNGAAENDE_DOKUMENT.toString())) {
      // Match on Mottaker
      return korrespondansepart.getKorrespondanseparttype().toLowerCase().endsWith("mottaker");
    } else if (journalpost
        .getJournalposttype()
        .equals(JournalpostDTO.JournalposttypeEnum.UTGAAENDE_DOKUMENT.toString())) {
      // Match on Avsender
      return korrespondansepart.getKorrespondanseparttype().toLowerCase().endsWith("avsender");
    } else {
      return true;
    }
  }

  /**
   * Sort two Korrespondansepart so that those with a korrespondanseparttype containing "intern" end
   * up last
   */
  private int sortRegularKorrespondansepartBeforeInternal(
      Korrespondansepart k1, Korrespondansepart k2) {
    return Boolean.compare(
        k1.getKorrespondanseparttype().contains("intern"),
        k2.getKorrespondanseparttype().contains("intern"));
  }

  /**
   * @param journalpostId The journalpost ID
   * @param query The query parameters
   * @return The list of Korrespondansepart objects
   */
  public PaginatedList<KorrespondansepartDTO> listKorrespondansepart(
      String journalpostId, ListByJournalpostParameters query) throws EInnsynException {
    query.setJournalpostId(journalpostId);
    return korrespondansepartService.list(query);
  }

  /**
   * @param journalpostId The journalpost ID
   * @param dto The KorrespondansepartDTO object
   * @return The KorrespondansepartDTO object
   */
  public KorrespondansepartDTO addKorrespondansepart(
      String journalpostId, KorrespondansepartDTO dto) throws EInnsynException {
    var journalpostDTO = journalpostService.get(journalpostId);
    dto.setJournalpost(new ExpandableField<>(journalpostDTO));
    var korrespondansepartDTO = korrespondansepartService.add(dto);
    var journalpost = journalpostService.findByIdOrThrow(journalpostId);
    journalpostService.updateAdmEnhetFromKorrPartList(journalpost);
    // We have to generate the DTO again here, in case the parent is expanded
    return korrespondansepartService.get(korrespondansepartDTO.getId());
  }

  /**
   * @param journalpostId The journalpost ID
   * @param query The query parameters
   * @return The list of Dokumentbeskrivelse objects
   */
  public PaginatedList<DokumentbeskrivelseDTO> listDokumentbeskrivelse(
      String journalpostId, ListByJournalpostParameters query) throws EInnsynException {
    query.setJournalpostId(journalpostId);
    return dokumentbeskrivelseService.list(query);
  }

  /**
   * Add a new dokumentbeskrivelse, or relate an existing one
   *
   * @param journalpostId
   * @param dto
   * @return
   * @throws EInnsynException
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable(
      retryFor = {ObjectOptimisticLockingFailureException.class},
      backoff = @Backoff(delay = 100, random = true))
  public DokumentbeskrivelseDTO addDokumentbeskrivelse(
      String journalpostId, ExpandableField<DokumentbeskrivelseDTO> dokumentbeskrivelseField)
      throws EInnsynException {

    var dokumentbeskrivelse =
        dokumentbeskrivelseService.createOrReturnExisting(dokumentbeskrivelseField);
    var journalpost = journalpostService.findByIdOrThrow(journalpostId);
    journalpost.addDokumentbeskrivelse(dokumentbeskrivelse);
    journalpostService.scheduleIndex(journalpostId, -1);

    var expandPaths =
        ExpandPathResolver.resolve(dokumentbeskrivelseField.getExpandedObject()).stream().toList();
    var query = new GetParameters();
    query.setExpand(expandPaths);

    return dokumentbeskrivelseService.get(dokumentbeskrivelse.getId(), query);
  }

  /**
   * Unrelates a Dokumentbeskrivelse from a Journalpost. The Dokumentbeskrivelse is deleted if it is
   * orphaned after the unrelate.
   *
   * @param journalpostId The journalpost ID
   * @param dokumentbeskrivelseId The dokumentbeskrivelse ID
   * @return The JournalpostDTO object
   */
  @Transactional(rollbackFor = Exception.class)
  @Retryable
  public DokumentbeskrivelseDTO deleteDokumentbeskrivelse(
      String journalpostId, String dokumentbeskrivelseId) throws EInnsynException {
    var journalpost = journalpostService.findByIdOrThrow(journalpostId);
    var dokumentbeskrivelseList = journalpost.getDokumentbeskrivelse();
    if (dokumentbeskrivelseList != null) {
      var updatedDokumentbeskrivelseList =
          dokumentbeskrivelseList.stream()
              .filter(dokbesk -> !dokbesk.getId().equals(dokumentbeskrivelseId))
              .toList();
      journalpost.setDokumentbeskrivelse(updatedDokumentbeskrivelseList);
    }
    var dokumentbeskrivelse = dokumentbeskrivelseService.findByIdOrThrow(dokumentbeskrivelseId);
    return dokumentbeskrivelseService.deleteIfOrphan(dokumentbeskrivelse);
  }

  /**
   * Uses legacy method to determine AdministrativEnhetKode from Korrespondansepart.
   *
   * @param journalpostId ID of the journalpost.
   * @return AdministrativEnhetKode or null.
   */
  public String getAdministrativEnhetKodeFromKorrespondansepart(@NotNull String journalpostId) {
    var journalpost = journalpostService.findById(journalpostId);
    if (journalpost == null) {
      return null;
    }
    return journalpost.getKorrespondansepart().stream()
        .filter(kp -> !kp.getKorrespondanseparttype().endsWith("kopimottaker"))
        .filter(kp -> korrespondansepartMatchesJournalpostDirection(journalpost, kp))
        .filter(kp -> kp.getAdministrativEnhet() != null)
        .filter(kp -> !kp.getAdministrativEnhet().trim().isEmpty())
        .filter(kp -> !kp.getAdministrativEnhet().toLowerCase().contains("ufordelt"))
        .sorted(this::sortRegularKorrespondansepartBeforeInternal)
        .map(Korrespondansepart::getAdministrativEnhet)
        .findFirst()
        .orElse(null);
  }
}
