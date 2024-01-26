// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.mappe.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.entities.arkiv.models.ArkivDTO;
import no.einnsyn.apiv3.entities.arkivdel.models.ArkivdelDTO;
import no.einnsyn.apiv3.entities.klasse.models.KlasseDTO;

@Getter
@Setter
public class UnionResourceParent implements HasId {

  String id;

  @Valid MappeDTO mappe;

  @Valid ArkivDTO arkiv;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  public UnionResourceParent(String id) {
    this.id = id;
  }

  public UnionResourceParent(MappeDTO mappe) {
    this.mappe = mappe;
    this.id = mappe.getId();
  }

  public UnionResourceParent(ArkivDTO arkiv) {
    this.arkiv = arkiv;
    this.id = arkiv.getId();
  }

  public UnionResourceParent(ArkivdelDTO arkivdel) {
    this.arkivdel = arkivdel;
    this.id = arkivdel.getId();
  }

  public UnionResourceParent(KlasseDTO klasse) {
    this.klasse = klasse;
    this.id = klasse.getId();
  }
}
