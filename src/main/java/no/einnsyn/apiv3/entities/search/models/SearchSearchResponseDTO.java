// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.search.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.moetemappe.models.Moetemappe;
import no.einnsyn.apiv3.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.entities.saksmappe.models.Saksmappe;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.apiv3.utils.IdGenerator;

@Getter
@Setter
public class SearchSearchResponseDTO implements HasId {

  String id;

  @Valid JournalpostDTO journalpost;

  @Valid MoetemappeDTO moetemappe;

  @Valid MoetesakDTO moetesak;

  @Valid SaksmappeDTO saksmappe;

  public SearchSearchResponseDTO(String id) {
    this.id = id;
  }

  public SearchSearchResponseDTO(JournalpostDTO journalpost) {
    this.journalpost = journalpost;
    this.id = journalpost.getId();
  }

  public SearchSearchResponseDTO(MoetemappeDTO moetemappe) {
    this.moetemappe = moetemappe;
    this.id = moetemappe.getId();
  }

  public SearchSearchResponseDTO(MoetesakDTO moetesak) {
    this.moetesak = moetesak;
    this.id = moetesak.getId();
  }

  public SearchSearchResponseDTO(SaksmappeDTO saksmappe) {
    this.saksmappe = saksmappe;
    this.id = saksmappe.getId();
  }

  public boolean isJournalpost() {
    return (journalpost != null || id.startsWith(IdGenerator.getPrefix(Journalpost.class)));
  }

  public boolean isMoetemappe() {
    return (moetemappe != null || id.startsWith(IdGenerator.getPrefix(Moetemappe.class)));
  }

  public boolean isMoetesak() {
    return (moetesak != null || id.startsWith(IdGenerator.getPrefix(Moetesak.class)));
  }

  public boolean isSaksmappe() {
    return (saksmappe != null || id.startsWith(IdGenerator.getPrefix(Saksmappe.class)));
  }
}
