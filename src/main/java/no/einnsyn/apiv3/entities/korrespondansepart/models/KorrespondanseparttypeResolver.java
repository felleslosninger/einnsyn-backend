package no.einnsyn.apiv3.entities.korrespondansepart.models;

import java.util.regex.Pattern;
import no.einnsyn.apiv3.error.exceptions.EInnsynException;

public class KorrespondanseparttypeResolver {

  private KorrespondanseparttypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  private static final String BASE = "http://www.arkivverket.no/standarder/noark5/arkivstruktur/";
  private static final Pattern PAT_AVSENDER = Pattern.compile(BASE + "avsender");
  private static final Pattern PAT_MOTTAKER = Pattern.compile(BASE + "mottaker");
  private static final Pattern PAT_KOPIMOTTAKER = Pattern.compile(BASE + "kopimottaker");
  private static final Pattern PAT_GRUPPEMOTTAKER = Pattern.compile(BASE + "gruppemottaker");
  private static final Pattern PAT_INTERN_AVSENDER = Pattern.compile(BASE + "intern_?[Aa]vsender");
  private static final Pattern PAT_INTERN_MOTTAKER = Pattern.compile(BASE + "intern_?[Mm]ottaker");
  private static final Pattern PAT_INTERN_KOPIMOTTAKER =
      Pattern.compile(BASE + "intern_?[Kk]opimottaker");

  public static KorrespondanseparttypeEnum resolve(String type) throws EInnsynException {
    type = type.toLowerCase();
    if (PAT_AVSENDER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.AVSENDER;
    }
    if (PAT_MOTTAKER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.MOTTAKER;
    }
    if (PAT_KOPIMOTTAKER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.KOPIMOTTAKER;
    }
    if (PAT_GRUPPEMOTTAKER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.GRUPPEMOTTAKER;
    }
    if (PAT_INTERN_AVSENDER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.INTERN_AVSENDER;
    }
    if (PAT_INTERN_MOTTAKER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.INTERN_MOTTAKER;
    }
    if (PAT_INTERN_KOPIMOTTAKER.matcher(type).matches()) {
      return KorrespondanseparttypeEnum.INTERN_KOPIMOTTAKER;
    }

    throw new EInnsynException("No korrespondanseparttype enum constant for value: " + type);
  }

  public static String fromIRI(String iri) {
    try {
      return resolve(iri).toString();
    } catch (EInnsynException e) {
      return iri;
    }
  }

  public static String toIRI(String type) {
    try {
      return BASE + resolve(type).toString();
    } catch (EInnsynException e) {
      return type;
    }
  }

  public static String toIRI(KorrespondanseparttypeEnum type) {
    return BASE + type.toString();
  }
}
