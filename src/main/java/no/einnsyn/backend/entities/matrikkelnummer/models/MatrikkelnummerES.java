package no.einnsyn.backend.entities.matrikkelnummer.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatrikkelnummerES {

  private String kommunenummer;
  private Integer gaardsnummer;
  private Integer bruksnummer;
  private Integer festenummer;
  private Integer seksjonsnummer;

  // Multiple search-friendly string variants per matrikkelnummer
  private List<String> matrikkelId;

  public static MatrikkelnummerES from(Matrikkelnummer m) {
    var es = new MatrikkelnummerES();
    es.setKommunenummer(m.getKommunenummer());
    es.setGaardsnummer(m.getGaardsnummer());
    es.setBruksnummer(m.getBruksnummer());
    es.setFestenummer(m.getFestenummer());
    es.setSeksjonsnummer(m.getSeksjonsnummer());
    es.setMatrikkelId(buildMatrikkelIds(m));
    return es;
  }

  private static List<String> buildMatrikkelIds(Matrikkelnummer m) {
    var k = m.getKommunenummer();
    var g = m.getGaardsnummer();
    var b = m.getBruksnummer();
    int f = m.getFestenummer() != null ? m.getFestenummer() : 0;
    int s = m.getSeksjonsnummer() != null ? m.getSeksjonsnummer() : 0;

    if (k == null || g == null || b == null) {
      return List.of();
    }

    var ids = new ArrayList<String>();
    // "10/99" — gaards/bruk utan kommune (vanlegaste søk)
    ids.add(g + "/" + b);
    // "0301-10/99" — standard Kartverket-format
    ids.add(k + "-" + g + "/" + b);
    // "0301/10/99" — alternativt format med berre skråstrek
    ids.add(k + "/" + g + "/" + b);
    // "0301-10/99/0/0" — fullformat med feste og seksjon
    ids.add(k + "-" + g + "/" + b + "/" + f + "/" + s);
    // "0301/10/99/0/0" — fullformat alternativt
    ids.add(k + "/" + g + "/" + b + "/" + f + "/" + s);
    return ids;
  }
}
