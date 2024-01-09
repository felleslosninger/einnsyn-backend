// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.mappe.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;
import no.einnsyn.apiv3.entities.mappe.models.MappeDTO;

@Getter
@Setter
public class UnionPropertyParent {

  private String id;

  @Valid
  private MappeDTO mappe;

  @Valid
  private ArkivDTO arkiv;

  @Valid
  private ArkivdelDTO arkivdel;

  @Valid
  private KlasseDTO klasse;

  public UnionPropertyParent(String id) {
    this.id = id;
  }
}
