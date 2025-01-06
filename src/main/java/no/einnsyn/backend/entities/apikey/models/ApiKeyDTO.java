// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.apikey.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.entities.base.models.BaseDTO;
import no.einnsyn.backend.entities.enhet.EnhetService;
import no.einnsyn.backend.entities.enhet.models.EnhetDTO;
import no.einnsyn.backend.validation.expandableobject.ExpandableObject;
import no.einnsyn.backend.validation.nossn.NoSSN;
import no.einnsyn.backend.validation.validationgroups.Insert;
import no.einnsyn.backend.validation.validationgroups.Update;

@Getter
@Setter
public class ApiKeyDTO extends BaseDTO {
  final String entity = "ApiKey";

  /**
   * A name for the API key. This can be used to identify the key, in case you have multiple keys
   * for multiple systems.
   */
  @NoSSN
  @Size(max = 500)
  String name;

  /**
   * The API key used to authenticate requests. This will only be shown once, and we will only store
   * a hashed version.
   */
  @NoSSN
  @Size(max = 500)
  @Null(groups = {Insert.class, Update.class})
  String secretKey;

  /** The Enhet that requests using this key will be associated with. */
  @ExpandableObject(
      service = EnhetService.class,
      groups = {Insert.class, Update.class})
  @Valid
  ExpandableField<EnhetDTO> enhet;
}
