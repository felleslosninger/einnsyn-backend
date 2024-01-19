package no.einnsyn.apiv3.entities.arkivbase;

import java.util.Set;
import no.einnsyn.apiv3.common.exceptions.EInnsynException;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBase;
import no.einnsyn.apiv3.entities.arkivbase.models.ArkivBaseDTO;
import no.einnsyn.apiv3.entities.base.BaseService;

public abstract class ArkivBaseService<O extends ArkivBase, D extends ArkivBaseDTO>
    extends BaseService<O, D> {

  // Temporarily use Oslo Kommune, since they have lots of subunits for testing. This will be
  // replaced by the logged in user's unit.
  public static String TEMPORARY_ADM_ENHET_ID = "enhet_01haf8swcbeaxt7s6spy92r7mq";

  protected abstract ArkivBaseRepository<O> getRepository();

  /**
   * @param id
   * @return
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
   * @param id
   * @return
   */
  @Override
  public boolean existsById(String id) {
    // If the ID doesn't start with our prefix, it is an external ID or a system ID
    if (!id.startsWith(idPrefix)) {
      // TODO: Should we have a systemId prefix?
      var exists = getRepository().existsBySystemId(id);
      if (exists) {
        return true;
      }
    }
    return super.existsById(id);
  }

  /**
   * Create a Base object from a DTO
   *
   * @param object
   * @param dto
   */
  @Override
  public O fromDTO(D dto, O object, Set<String> paths, String currentPath) throws EInnsynException {

    // externalId can't start with idPrefix, this will break ID lookups
    var externalId = dto.getExternalId();
    if (externalId != null && !externalId.startsWith(idPrefix)) {
      object.setExternalId(dto.getExternalId());
    }

    // This is an insert. Find journalenhet from authentication
    if (object.getId() == null) {
      // TODO: Fetch journalenhet from authentication
      var journalEnhet = enhetService.findById(TEMPORARY_ADM_ENHET_ID);
      object.setJournalenhet(journalEnhet);
    }

    return super.fromDTO(dto, object, paths, currentPath);
  }

  @Override
  public D toDTO(O object, D dto, Set<String> expandPaths, String currentPath) {
    dto.setExternalId(object.getExternalId());

    var journalenhet = object.getJournalenhet();
    if (journalenhet != null) {
      dto.setJournalenhet(
          enhetService.maybeExpand(journalenhet, "journalenhet", expandPaths, currentPath));
    }

    return super.toDTO(object, dto, expandPaths, currentPath);
  }
}
