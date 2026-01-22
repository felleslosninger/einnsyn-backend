package no.einnsyn.backend.common.search;

import co.elastic.clients.elasticsearch._types.FieldValue;

public class SortByMapper {

  public static String resolve(String sortBy) {
    return switch (sortBy) {
      case "administrativEnhetNavn" -> "arkivskaperSorteringNavn";
      case "dokumentetsDato" -> "dokumentetsDato";
      case "entity" -> "type";
      case "fulltekst" -> "fulltext";
      case "id" -> "id";
      case "journaldato" -> "journaldato";
      case "journalpostnummer" -> "journalpostnummer_sort";
      case "journalposttype" -> "journalposttype";
      case "korrespondansepartNavn" -> "search_korrespodansepart_sort";
      case "moetedato" -> "moetedato";
      case "oppdatertDato" -> "oppdatertDato";
      case "publisertDato" -> "publisertDato";
      case "sakssekvensnummer" -> "sakssekvensnummer_sort";
      case "score" -> "_score";
      case "tittel" -> "search_tittel_sort";
      case "type" -> "sorteringstype";
      default -> null;
    };
  }

  public static FieldValue toFieldValue(String sortBy, String value) {
    return switch (sortBy) {
      case "administrativEnhetNavn" -> FieldValue.of(value);
      case "dokumentetsDato" -> FieldValue.of(Long.parseLong(value));
      case "entity" -> FieldValue.of(value);
      case "fulltekst" -> FieldValue.of(Boolean.parseBoolean(value));
      case "id" -> FieldValue.of(value);
      case "journaldato" -> FieldValue.of(Long.parseLong(value));
      case "journalpostnummer" -> FieldValue.of(value);
      case "journalposttype" -> FieldValue.of(value);
      case "korrespondansepartNavn" -> FieldValue.of(value);
      case "moetedato" -> FieldValue.of(Long.parseLong(value));
      case "oppdatertDato" -> FieldValue.of(Long.parseLong(value));
      case "publisertDato" -> FieldValue.of(Long.parseLong(value));
      case "sakssekvensnummer" -> FieldValue.of(Integer.parseInt(value));
      case "score" -> FieldValue.of(Double.parseDouble(value));
      case "tittel" -> FieldValue.of(value);
      case "type" -> FieldValue.of(value);
      default -> null;
    };
  }
}
