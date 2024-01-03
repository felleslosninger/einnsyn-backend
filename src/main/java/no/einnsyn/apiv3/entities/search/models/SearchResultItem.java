package no.einnsyn.apiv3.entities.search.models;

import lombok.Getter;
import no.einnsyn.apiv3.entities.journalpost.models.JournalpostJSON;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeJSON;

@Getter
public class SearchResultItem {

  private JournalpostJSON journalpostJSON;

  private SaksmappeJSON saksmappeJSON;

  public SearchResultItem(JournalpostJSON journalpostJSON) {
    this.journalpostJSON = journalpostJSON;
  }

  public SearchResultItem(SaksmappeJSON saksmappeJSON) {
    this.saksmappeJSON = saksmappeJSON;
  }
}
