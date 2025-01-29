// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.base.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime.Format;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class BaseDTO implements HasId {
  /**
   * The unique identifier for the resource. This is is assigned by the system when the resource is
   * created.
   */
  @Null(groups = {Insert.class, Update.class})
  String id;

  /** A timestamp of when the resource was created. This field is updated automatically. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String created;

  /** A timestamp of when the resource was last updated. This field is updated automatically. */
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String updated;

  /**
   * This field is only present if the resource has been deleted. If present, it will always be
   * `true`.
   */
  @Null(groups = {Insert.class, Update.class})
  Boolean deleted;

  /**
   * An external ID for the resource. This is similar to "systemId", but will be used for legacy
   * IRIs that were used in earlier eInnsyn versions.
   */
  @NoSSN
  @Size(max = 500)
  String externalId;

  @Size(max = 500)
  @IsoDateTime(format = Format.ISO_DATE)
  String accessibleAfter;
}
