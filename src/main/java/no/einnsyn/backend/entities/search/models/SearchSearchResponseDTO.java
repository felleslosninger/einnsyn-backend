// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.backend.entities.search.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetemappe.models.Moetemappe;
import no.einnsyn.backend.entities.moetemappe.models.MoetemappeDTO;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.entities.saksmappe.models.Saksmappe;
import no.einnsyn.backend.entities.saksmappe.models.SaksmappeDTO;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;

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

  public SearchSearchResponseDTO(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    HasId obj = expandableField.getExpandedObject();
    if (obj instanceof JournalpostDTO typedObj) {
      this.journalpost = typedObj;
    }
    if (obj instanceof MoetemappeDTO typedObj) {
      this.moetemappe = typedObj;
    }
    if (obj instanceof MoetesakDTO typedObj) {
      this.moetesak = typedObj;
    }
    if (obj instanceof SaksmappeDTO typedObj) {
      this.saksmappe = typedObj;
    }
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
    return (journalpost != null || id.startsWith(IdGenerator.getPrefix(Journalpost.class) + "_"));
  }

  public boolean isMoetemappe() {
    return (moetemappe != null || id.startsWith(IdGenerator.getPrefix(Moetemappe.class) + "_"));
  }

  public boolean isMoetesak() {
    return (moetesak != null || id.startsWith(IdGenerator.getPrefix(Moetesak.class) + "_"));
  }

  public boolean isSaksmappe() {
    return (saksmappe != null || id.startsWith(IdGenerator.getPrefix(Saksmappe.class) + "_"));
  }
}
