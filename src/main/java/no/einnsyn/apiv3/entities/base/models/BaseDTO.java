// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.base.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.validation.nossn.NoSSN;
import no.einnsyn.apiv3.validation.validationgroups.Insert;
import no.einnsyn.apiv3.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public abstract class BaseDTO implements HasId {

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  String id;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String created;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  String updated;

  @Null(groups = {Insert.class, Update.class})
  Boolean deleted;

  @Size(max = 500)
  @NoSSN
  String externalId;
}
