// Auto-generated from our OpenAPI spec
// https://github.com/felleslosninger/ein-openapi/

package no.einnsyn.apiv3.entities.korrespondansepart.models;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.common.expandablefield.ExpandableField;
import no.einnsyn.apiv3.common.hasid.HasId;
import no.einnsyn.apiv3.entities.journalpost.models.Journalpost;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.apiv3.entities.moetedokument.models.Moetedokument;
import no.einnsyn.apiv3.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.apiv3.entities.moetesak.models.Moetesak;
import no.einnsyn.apiv3.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.apiv3.utils.idgenerator.IdGenerator;

@Getter
@Setter
public class KorrespondansepartParentDTO implements HasId {

  String id;

  @Valid JournalpostDTO journalpost;

  @Valid MoetedokumentDTO moetedokument;

  @Valid MoetesakDTO moetesak;

  public KorrespondansepartParentDTO(String id) {
    this.id = id;
  }

  public KorrespondansepartParentDTO(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    HasId obj = expandableField.getExpandedObject();
    if (obj instanceof JournalpostDTO typedObj) {
      this.journalpost = typedObj;
    }
    if (obj instanceof MoetedokumentDTO typedObj) {
      this.moetedokument = typedObj;
    }
    if (obj instanceof MoetesakDTO typedObj) {
      this.moetesak = typedObj;
    }
  }

  public KorrespondansepartParentDTO(JournalpostDTO journalpost) {
    this.journalpost = journalpost;
    this.id = journalpost.getId();
  }

  public KorrespondansepartParentDTO(MoetedokumentDTO moetedokument) {
    this.moetedokument = moetedokument;
    this.id = moetedokument.getId();
  }

  public KorrespondansepartParentDTO(MoetesakDTO moetesak) {
    this.moetesak = moetesak;
    this.id = moetesak.getId();
  }

  public boolean isJournalpost() {
    return (journalpost != null || id.startsWith(IdGenerator.getPrefix(Journalpost.class) + "_"));
  }

  public boolean isMoetedokument() {
    return (moetedokument != null
        || id.startsWith(IdGenerator.getPrefix(Moetedokument.class) + "_"));
  }

  public boolean isMoetesak() {
    return (moetesak != null || id.startsWith(IdGenerator.getPrefix(Moetesak.class) + "_"));
  }
}
