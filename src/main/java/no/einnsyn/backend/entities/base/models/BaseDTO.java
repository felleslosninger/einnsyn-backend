// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.base.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.validation.isodatetime.IsoDateTime;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public abstract class BaseDTO implements HasId {

  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  String id;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String created;

  @Size(max = 500)
  @IsoDateTime(format = IsoDateTime.Format.ISO_DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String updated;

  @Null(groups = {Insert.class, Update.class})
  Boolean deleted;

  @Size(max = 500)
  String externalId;

  Boolean hidden;
}
