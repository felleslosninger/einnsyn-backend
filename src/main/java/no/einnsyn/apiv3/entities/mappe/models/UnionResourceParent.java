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
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;

@Getter
@Setter
public class UnionResourceParent implements HasId {

  String id;

  @Valid SaksmappeDTO saksmappe;

  @Valid MoetemappeDTO moetemappe;

  @Valid ArkivDTO arkiv;

  @Valid ArkivdelDTO arkivdel;

  @Valid KlasseDTO klasse;

  public UnionResourceParent(String id) {
    this.id = id;
  }

  public UnionResourceParent(SaksmappeDTO saksmappe) {
    this.saksmappe = saksmappe;
    this.id = saksmappe.getId();
  }

  public UnionResourceParent(MoetemappeDTO moetemappe) {
    this.moetemappe = moetemappe;
    this.id = moetemappe.getId();
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
