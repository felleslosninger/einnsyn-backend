// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.arkivdel.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.queryparameters.models.ListParameters;
import no.einnsyn.backend.validation.validationgroups.Insert;

@Getter
@Setter
public class ListByArkivdelParameters extends ListParameters {
  @NotBlank(groups = {Insert.class})
  String id;

  @NotBlank(groups = {Insert.class})
  String arkivdelId;
}
