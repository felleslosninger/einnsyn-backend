package no.einnsyn.apiv3.entities.journalpost.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.apiv3.entities.saksmappe.models.SaksmappeES;

@Getter
@Setter
public class JournalpostES extends JournalpostDTO {

  List<String> type = List.of("Journalpost");

  @SuppressWarnings("java:S116")
  String offentligTittel_SENSITIV;

  List<String> avsender;

  @SuppressWarnings("java:S116")
  List<String> avsender_SENSITIV;

  List<String> mottaker;

  @SuppressWarnings("java:S116")
  List<String> mottaker_SENSITIV;

  List<String> arkivskaperTransitive;

  List<String> arkivskaperNavn;

  String arkivskaper;

  String arkivskaperSorteringNavn;

  SaksmappeES parent;
}
