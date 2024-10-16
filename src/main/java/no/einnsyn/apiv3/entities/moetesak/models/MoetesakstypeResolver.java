package no.einnsyn.apiv3.entities.moetesak.models;

import java.util.regex.Pattern;

public class MoetesakstypeResolver {

  private MoetesakstypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  // Pre-compile patterns for matching moetesak types
  private static final String OE = "(ø|o|%c3%b8|%c3%98)";
  private static final Pattern PAT_MOETE =
      Pattern.compile(
          ".*m" + OE + "tesakstype_annet|.*Notater\\+og\\+orienteringer.*", Pattern.CANON_EQ);
  private static final Pattern PAT_POLITISK =
      Pattern.compile(".*/[pP]olitisk[+]?[sS]ak|.*/Klagenemnda|.*/Faste\\+saker|.*/Byr.*dssak");
  private static final Pattern PAT_DELEGERT =
      Pattern.compile(".*/delegertSak|.*/Administrativ\\+sak");
  private static final Pattern PAT_INTERPELLASJON = Pattern.compile(".*/interpellasjon");
  private static final Pattern PAT_GODKJENNING = Pattern.compile(".*/godkjenning");
  private static final Pattern PAT_ORIENTERING = Pattern.compile(".*/orienteringssak");
  private static final Pattern PAT_REFERAT = Pattern.compile(".*/referatsak|.*/Referatsaker");

  public static MoetesakstypeEnum resolve(String type) {
    type = type.toLowerCase();
    if (PAT_MOETE.matcher(type).matches()) {
      return MoetesakstypeEnum.MOETE;
    }
    if (PAT_POLITISK.matcher(type).matches()) {
      return MoetesakstypeEnum.POLITISK;
    }
    if (PAT_DELEGERT.matcher(type).matches()) {
      return MoetesakstypeEnum.DELEGERT;
    }
    if (PAT_INTERPELLASJON.matcher(type).matches()) {
      return MoetesakstypeEnum.INTERPELLASJON;
    }
    if (PAT_GODKJENNING.matcher(type).matches()) {
      return MoetesakstypeEnum.GODKJENNING;
    }
    if (PAT_ORIENTERING.matcher(type).matches()) {
      return MoetesakstypeEnum.ORIENTERING;
    }
    if (PAT_REFERAT.matcher(type).matches()) {
      return MoetesakstypeEnum.REFERAT;
    }
    return MoetesakstypeEnum.ANNET;
  }
}
