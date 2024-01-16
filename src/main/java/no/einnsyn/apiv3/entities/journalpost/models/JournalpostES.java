package no.einnsyn.apiv3.entities.journalpost.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JournalpostES extends JournalpostDTO {

  List<String> type = List.of("Journalpost");

  String offentligTittel_SENSITIV;

  List<String> avsender;

  List<String> avsender_SENSITIV;

  List<String> mottaker;

  List<String> mottaker_SENSITIV;

  List<String> arkivskaperTransitive;

  List<String> arkivskaperNavn;

  String arkivskaper;

  String arkivskaperSorteringNavn;
}
