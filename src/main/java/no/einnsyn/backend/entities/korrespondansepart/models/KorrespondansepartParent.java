// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.entities.korrespondansepart.models;

import jakarta.validation.Valid;
import lombok.Getter;
import no.einnsyn.backend.common.expandablefield.ExpandableField;
import no.einnsyn.backend.common.hasid.HasId;
import no.einnsyn.backend.entities.journalpost.models.Journalpost;
import no.einnsyn.backend.entities.journalpost.models.JournalpostDTO;
import no.einnsyn.backend.entities.moetedokument.models.Moetedokument;
import no.einnsyn.backend.entities.moetedokument.models.MoetedokumentDTO;
import no.einnsyn.backend.entities.moetesak.models.Moetesak;
import no.einnsyn.backend.entities.moetesak.models.MoetesakDTO;
import no.einnsyn.backend.utils.idgenerator.IdGenerator;

@Getter
public class KorrespondansepartParent implements HasId {
  @Valid String id;

  @Valid JournalpostDTO journalpost;

  @Valid MoetedokumentDTO moetedokument;

  @Valid MoetesakDTO moetesak;

  public KorrespondansepartParent(String id) {
    this.id = id;
  }

  public KorrespondansepartParent(JournalpostDTO journalpost) {
    this.journalpost = journalpost;
  }

  public KorrespondansepartParent(MoetedokumentDTO moetedokument) {
    this.moetedokument = moetedokument;
  }

  public KorrespondansepartParent(MoetesakDTO moetesak) {
    this.moetesak = moetesak;
  }

  public KorrespondansepartParent(ExpandableField<?> expandableField) {
    this.id = expandableField.getId();
    var obj = expandableField.getExpandedObject();
    if (obj == null) return;
    switch (obj) {
      case JournalpostDTO typedObj -> this.journalpost = typedObj;
      case MoetedokumentDTO typedObj -> this.moetedokument = typedObj;
      case MoetesakDTO typedObj -> this.moetesak = typedObj;
      default ->
          throw new IllegalArgumentException(
              "Unsupported object type: " + obj.getClass().getName());
    }
  }

  public boolean isJournalpost() {
    return journalpost != null || id.startsWith(IdGenerator.getPrefix(Journalpost.class) + "_");
  }

  public boolean isMoetedokument() {
    return moetedokument != null || id.startsWith(IdGenerator.getPrefix(Moetedokument.class) + "_");
  }

  public boolean isMoetesak() {
    return moetesak != null || id.startsWith(IdGenerator.getPrefix(Moetesak.class) + "_");
  }
}
