// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api-spec

package no.einnsyn.backend.entities.arkivbase.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

/** Properties shared by all Noark objects */
@Getter
@Setter
public class ArkivBaseDTO extends BaseDTO {
  /**
   * An identifier for the resource, given by the user's system.
   *
   * <p>For most entities the systemId is unique, and can be used in place of the eInnsynId when
   * looking up a single object. It is *not* unique for Arkiv, Arkivdel and Klasse, and can not be
   * used to look those up.
   */
  protected String systemId;

  /**
   * The administrative unit that is responsible for the resource. This is by default derived from
   * the credentials used to authenticate the request on creation, or it can manually be set to an
   * Enhet owned by that derived Enhet.
   */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  protected ExpandableField<EnhetDTO> journalenhet;
}
