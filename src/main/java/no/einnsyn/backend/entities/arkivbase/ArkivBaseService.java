package no.einnsyn.backend.entities.arkivbase;

import java.util.ArrayList;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.einnsyn.backend.common.exceptions.models.AuthorizationException;
import no.einnsyn.backend.common.exceptions.models.EInnsynException;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBase;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.backend.entities.arkivbase.models.ArkivBaseES;
import no.einnsyn.backend.entities.base.BaseService;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.base.models.BaseES;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
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
    if (!id.startsWith(idPrefix)
        && !objectClassName.equals("Arkiv")
        && !objectClassName.equals("Arkivdel")) {
      var object = getRepository().findBySystemId(id);
      if (object != null) {
        return object;
      }
    }

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
  public Pair<String, O> findPropertyAndObjectByDTO(BaseDTO baseDTO) {

    if (baseDTO instanceof ArkivBaseDTO dto
        && dto.getSystemId() != null
        && !objectClassName.equals("Arkiv")
        && !objectClassName.equals("Arkivdel")) {
      var obj = this.getRepository().findBySystemId(dto.getSystemId());
      if (obj != null) {
        return Pair.of("systemId", obj);
      }
    }

    return super.findPropertyAndObjectByDTO(baseDTO);
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
      var wantedJournalenhet =
          enhetService.findByIdOrThrow(dto.getJournalenhet().getId(), AuthorizationException.class);

      if (!enhetService.isAncestorOf(
          authenticationService.getEnhetId(), wantedJournalenhet.getId())) {
        throw new AuthorizationException(
            "Not authorized to set journalenhet to " + wantedJournalenhet.getId());
      }
      object.setJournalenhet(wantedJournalenhet);
    }

    // This is an insert. Find journalenhet from authentication
    if (object.getId() == null && object.getJournalenhet() == null) {
      var journalenhetId = authenticationService.getEnhetId();
      if (journalenhetId == null) {
        throw new AuthorizationException(
            "Not authenticated to add " + objectClassName + " without a journalenhet.");
      }
      var journalenhet = enhetService.findByIdOrThrow(journalenhetId, AuthorizationException.class);
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
        var administrativEnhetTransitive = new ArrayList<String>();
        var arkivskaperTransitive = new ArrayList<String>();
        var arkivskaperNavn = new ArrayList<String>();
        for (var transitiveEnhet : transitiveEnhets) {
          administrativEnhetTransitive.add(transitiveEnhet.getId());
          arkivskaperTransitive.add(transitiveEnhet.getIri());
          arkivskaperNavn.add(transitiveEnhet.getNavn().trim());
        }

        arkivBaseES.setAdministrativEnhet(enhet.getId());
        arkivBaseES.setAdministrativEnhetTransitive(administrativEnhetTransitive);
        arkivBaseES.setArkivskaper(enhet.getIri());
        arkivBaseES.setArkivskaperTransitive(arkivskaperTransitive);
        arkivBaseES.setArkivskaperNavn(arkivskaperNavn);
        arkivBaseES.setArkivskaperSorteringNavn(
            arkivskaperNavn.isEmpty() ? "" : arkivskaperNavn.getFirst().trim());
      }
    }
    return es;
  }

  /**
   * Check if the authenticated user is the owner of an Object.
   *
   * @param object the object to check ownership for
   * @return true if the authenticated user is the owner
   */
  @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
  public boolean isOwnerOf(O object) {
    var loggedInAs = authenticationService.getEnhetId();
    if (loggedInAs == null) {
      return false;
    }

    var journalenhet = object.getJournalenhet();
    if (journalenhet == null) {
      return false;
    }

    return enhetService.isAncestorOf(loggedInAs, journalenhet.getId());
  }

  /** Authorize the list operation. By default, anybody can list ArkivBase objects. */
  @Override
  protected void authorizeList(ListParameters params) {}

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
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeAdd(D dto) throws EInnsynException {
    if (authenticationService.getEnhetId() == null) {
      throw new AuthorizationException("Not authenticated to add " + objectClassName + ".");
    }
  }

  /**
   * Authorize the update operation. Only users representing a journalenhet that owns the object can
   * update.
   *
   * @param id The ID of the object to update
   * @param dto The DTO to update from
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  protected void authorizeUpdate(String id, D dto) throws EInnsynException {
    var loggedInAs = authenticationService.getEnhetId();
    if (loggedInAs == null) {
      throw new AuthorizationException("Not authenticated to update " + objectClassName + ".");
    }
    var wantsToUpdate = getProxy().findByIdOrThrow(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToUpdate.getJournalenhet().getId())) {
      throw new AuthorizationException("Not authorized to update " + id);
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
   * @throws AuthorizationException If the user is not authorized
   */
  @Override
  public void authorizeDelete(String id) throws EInnsynException {
    var loggedInAs = authenticationService.getEnhetId();
    if (loggedInAs == null) {
      throw new AuthorizationException(
          "Not authenticated to delete "
              + objectClassName
              + " : "
              + id
              + " without a journalenhet. (Not logged in?)");
    }
    var wantsToDelete = getProxy().findByIdOrThrow(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToDelete.getJournalenhet().getId())) {
      throw new AuthorizationException("Not authorized to delete " + objectClassName + " : " + id);
    }
  }
}
