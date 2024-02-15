package no.einnsyn.apiv3.entities.saksmappe.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaksmappeES extends SaksmappeDTO {
  List<String> type = List.of("Saksmappe");

  String offentligTittel_SENSITIV;

  List<String> saksnummerGenerert;

  List<String> arkivskaperTransitive;

  List<String> arkivskaperNavn;

  String arkivskaper;

  String arkivskaperSorteringNavn;
}
