// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.base.models;

import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.features.validation.nossn.NoSSN;
import no.einnsyn.apiv3.features.validation.validationgroups.Insert;
import no.einnsyn.apiv3.features.validation.validationgroups.Update;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public abstract class BaseDTO {

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  private String id;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  private String created;

  @Size(max = 500)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Null(groups = {Insert.class, Update.class})
  private String updated;

  @Null(groups = {Insert.class, Update.class})
  private Boolean deleted;

  @Size(max = 500)
  @NoSSN
  @Null(groups = {Insert.class, Update.class})
  private String entity;
}
