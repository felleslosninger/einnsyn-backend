// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;

@Getter
@Setter
public class UnionResourceSearch implements HasId {

  String id;

  @Valid JournalpostDTO journalpost;

  @Valid MoetemappeDTO moetemappe;

  @Valid MoetesakDTO moetesak;

  @Valid SaksmappeDTO saksmappe;

  public UnionResourceSearch(String id) {
    this.id = id;
  }

  public UnionResourceSearch(JournalpostDTO journalpost) {
    this.journalpost = journalpost;
    this.id = journalpost.getId();
  }

  public UnionResourceSearch(MoetemappeDTO moetemappe) {
    this.moetemappe = moetemappe;
    this.id = moetemappe.getId();
  }

  public UnionResourceSearch(MoetesakDTO moetesak) {
    this.moetesak = moetesak;
    this.id = moetesak.getId();
  }

  public UnionResourceSearch(SaksmappeDTO saksmappe) {
    this.saksmappe = saksmappe;
    this.id = saksmappe.getId();
  }
}
