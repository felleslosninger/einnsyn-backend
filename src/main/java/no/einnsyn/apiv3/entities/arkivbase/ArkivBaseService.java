package no.einnsyn.apiv3.entities.arkivbase;

import jakarta.transaction.Transactional;
import java.util.Set;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.base.BaseService;
import no.einnsyn.apiv3.entities.base.models.BaseListQueryDTO;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;
import no.einnsyn.apiv3.error.exceptions.ForbiddenException;

@SuppressWarnings("java:S1192") // Allow multiple string literals
public abstract class ArkivBaseService<O extends ArkivBase, D extends ArkivBaseDTO>
    extends BaseService<O, D> {

  protected abstract ArkivBaseRepository<O> getRepository();

  /**
   * @param id The ID of the object to find
   * @return The object with the given ID, or null if not found
   */
  @Override
  public O findById(String id) {
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      // TODO: Should we have a systemId prefix?
      var object = getRepository().findBySystemId(id);
      if (object != null) {
        return object;
      }
    }
    return super.findById(id);
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

    // externalId can't start with idPrefix, this will break ID lookups
    var externalId = dto.getExternalId();
    if (externalId != null && !externalId.startsWith(idPrefix)) {
      object.setExternalId(dto.getExternalId());
    }

    // This is an insert. Find journalenhet from authentication
    if (object.getId() == null) {
      var journalenhetId = authenticationService.getJournalenhetId();
      if (journalenhetId == null) {
        throw new ForbiddenException("Could not get journalenhet from authentication.");
      }
      var journalenhet = enhetService.findById(journalenhetId);
      if (journalenhet == null) {
        throw new ForbiddenException("Could not find journalenhet " + journalenhetId);
      }
      object.setJournalenhet(journalenhet);
    }

    return object;
  }

  @Override
  protected D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {
    dto.setExternalId(object.getExternalId());

    var journalenhet = object.getJournalenhet();
    if (journalenhet != null) {
      dto.setJournalenhet(
          enhetService.maybeExpand(journalenhet, "journalenhet", expandPaths, currentPath));
    }

    return super.toDTO(object, dto, expandPaths, currentPath);
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
  protected void authorizeAdd(D dto) throws ForbiddenException {
    if (authenticationService.getJournalenhetId() == null) {
      throw new ForbiddenException("Could not get journalenhet from authentication.");
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
  @Transactional
  protected void authorizeUpdate(String id, D dto) throws ForbiddenException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (loggedInAs == null) {
      throw new ForbiddenException("Could not get journalenhet from authentication.");
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
   * @param id The ID of the object to delete
   * @throws ForbiddenException If the user is not authorized
   */
  @Override
  @Transactional
  protected void authorizeDelete(String id) throws ForbiddenException {
    var loggedInAs = authenticationService.getJournalenhetId();
    if (loggedInAs == null) {
      throw new ForbiddenException("Could not get journalenhet from authentication.");
    }
    var wantsToDelete = getProxy().findById(id);
    if (!enhetService.isAncestorOf(loggedInAs, wantsToDelete.getJournalenhet().getId())) {
      throw new ForbiddenException("Not authorized to delete " + id);
    }
  }
}
