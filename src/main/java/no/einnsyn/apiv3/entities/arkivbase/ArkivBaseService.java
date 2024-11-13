package no.einnsyn.apiv3.entities.arkivbase;

import java.util.ArrayList;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseDTO;
import no.einnsyn.apiv3.entities.base.models.BaseES;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("java:S1192") // Allow multiple string literals
@Slf4j
public abstract class ArkivBaseService<O extends ArkivBase, D extends ArkivBaseDTO>
    extends BaseService<O, D> {

  protected abstract ArkivBaseRepository<O> getRepository();

  /**
   * @param id The ID of the object to find
   * @return The object with the given ID, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public O findById(String id) {

    // TODO: We currently can't have unique constraints on Arkiv.systemId and Arkivdel.systemId.
    // Enable this when we can:
    // if (!id.startsWith(idPrefix)) {
    //   var object = getRepository().findBySystemId(id);
    //   if (object != null) {
    //     return object;
    //   }
    // }

    return super.findById(id);
  }

  /**
   * Extend findByDTO to also look for systemId
   *
   * @param baseDTO The DTO to find
   * @return The object with the given system ID, or null if not found
   */
  @Override
  @Transactional(readOnly = true)
  public O findByDTO(BaseDTO baseDTO) {

    if (baseDTO instanceof ArkivBaseDTO dto && dto.getSystemId() != null) {
      var found = this.getRepository().findBySystemId(dto.getSystemId());
      if (found != null) {
        return found;
      }
    }

    return super.findByDTO(baseDTO);
  }

  /**
   * Create a Base object from a DTO
   *
   * @param object The object to update
   * @param dto The DTO to update from
   */
  @Override
  protected O fromDTO(D dto, O object) throws EInnsynException {
    super.fromDTO(dto, object);

    // Users can set the journalenhet to Enhets that they own
    if (dto.getJournalenhet() != null) {
      var wantedJournalenhet = enhetService.findById(dto.getJournalenhet().getId());
      if (wantedJournalenhet == null) {
        throw new ForbiddenException(
            "Could not find journalenhet " + dto.getJournalenhet().getId());
      }
      if (!enhetService.isAncestorOf(
          authenticationService.getJournalenhetId(), wantedJournalenhet.getId())) {
        throw new ForbiddenException(
            "Not authorized to set journalenhet to " + wantedJournalenhet.getId());
      }
      object.setJournalenhet(wantedJournalenhet);
    }

    // This is an insert. Find journalenhet from authentication
    if (object.getId() == null && object.getJournalenhet() == null) {
      var journalenhetId = authenticationService.getJournalenhetId();
      if (journalenhetId == null) {
        throw new ForbiddenException(
            "Not authenticated to add " + objectClassName + " without a journalenhet.");
      }
      var journalenhet = enhetService.findById(journalenhetId);
      if (journalenhet == null) {
        throw new ForbiddenException(
            "Could not find journalenhet " + journalenhetId + " when adding " + objectClassName);
      }
      object.setJournalenhet(journalenhet);
    }

    if (dto.getSystemId() != null) {
      object.setSystemId(dto.getSystemId());
    }

    return object;
  }

  @Override
  protected D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {
    var journalenhet = object.getJournalenhet();
    if (journalenhet != null) {
      dto.setJournalenhet(
          enhetService.maybeExpand(journalenhet, "journalenhet", expandPaths, currentPath));
    }

    dto.setSystemId(object.getSystemId());

    return super.toDTO(object, dto, expandPaths, currentPath);
  }

  @Override
  protected BaseES toLegacyES(O object, BaseES es) {
    super.toLegacyES(object, es);
    if (es instanceof ArkivBaseES arkivBaseES) {
      var enhet =
          switch (object) {
            case Saksmappe saksmappe -> saksmappe.getAdministrativEnhetObjekt();
            case Moetemappe moetemappe -> moetemappe.getUtvalgObjekt();
            case Journalpost journalpost -> journalpost.getAdministrativEnhetObjekt();
            case Moetesak moetesak ->
                moetesak.getMoetemappe() != null
                    ? moetesak.getMoetemappe().getUtvalgObjekt()
                    : moetesak.getUtvalgObjekt();
            case Moetedokument moetedokument -> moetedokument.getMoetemappe().getUtvalgObjekt();
            default -> object.getJournalenhet();
          };
      if (enhet == null) {
        log.error("No enhet found for {}:{}", objectClassName, object.getId());
      } else {
        var transitiveEnhets = enhetService.getTransitiveEnhets(enhet);
        var arkivskaperTransitive = new ArrayList<String>();
        var arkivskaperNavn = new ArrayList<String>();
        for (var transitiveEnhet : transitiveEnhets) {
          arkivskaperTransitive.add(transitiveEnhet.getIri());
          arkivskaperNavn.add(transitiveEnhet.getNavn());
        }
        arkivBaseES.setArkivskaper(enhet.getIri());
        arkivBaseES.setArkivskaperTransitive(arkivskaperTransitive);
        arkivBaseES.setArkivskaperNavn(arkivskaperNavn);
        arkivBaseES.setArkivskaperSorteringNavn(
            arkivskaperNavn.isEmpty() ? "" : arkivskaperNavn.getFirst());
      }
    }
    return es;
  }

  /** Authorize the list operation. By default, anybody can list ArkivBase objects. */
  @Override
  protected void authorizeList(BaseListQueryDTO params) {}

  /**
   * Authorize the get operation. By default, anybody can get ArkivBase objects.
   *
   * @param id The ID of the object to get
   */
  @Override
  protected void authorizeGet(String id) {}

  /**
   * Authorize the add operation. By default, only users with a journalenhet can add ArkivBase
   * objects.
   *
   * @param dto The DTO to add
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(D dto) throws EInnsynException {
    if (authenticationService.getJournalenhetId() == null) {
      throw new ForbiddenException("Not authenticated to add " + objectClassName + ".");
    }
  }

  /**
   * Authorize the update operation. Only users representing a journalenhet that owns the object can
   * update.
   *
   * @param id The ID of the object to update
   * @param dto The DTO to update from
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, D dto) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (loggedInAs == null) {
      throw new ForbiddenException("Not authenticated to update " + objectClassName + ".");
    }
    var wantsToUpdate = getProxy().findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToUpdate.getJournalenhet().getId())) {
      throw new ForbiddenException("Not authorized to update " + id);
    }
  }

  /**
   * Authorize the delete operation. Only users representing a journalenhet that owns the object can
   * delete.
   *
   * <p>This method is public to allow children elements to check if the user is allowed to delete a
   * parent, for example in LagretSak, where the owner of a Saksmappe can recursively delete
   * LagretSak.
   *
   * @param id The ID of the object to delete
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  public void authorizeDelete(String id) throws EInnsynException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (loggedInAs == null) {
      throw new ForbiddenException(
          "Not authenticated to delete "
              + objectClassName
              + " : "
              + id
              + " without a journalenhet. (Not logged in?)");
    }
    var wantsToDelete = getProxy().findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToDelete.getJournalenhet().getId())) {
      throw new ForbiddenException("Not authorized to delete " + objectClassName + " : " + id);
    }
  }
}
