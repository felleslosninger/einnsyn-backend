package no.einnsyn.backend.entities.moetesak.models;

import java.util.regex.Pattern;

public class MoetesakstypeResolver {

  private MoetesakstypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  private static final Pattern MOETE_PATTERN =
      compilePattern("møtesakstype annet", "Notater og orienteringer");
  private static final Pattern POLITISK_PATTERN =
      compilePattern("politisk sak", "klagenemnda", "faste saker", "byrådssak");
  private static final Pattern DELEGERT_PATTERN =
      compilePattern("delegert sak", "administrativ sak", "delegert");
  private static final Pattern INTERPELLASJON_PATTERN = compilePattern("interpellasjon");
  private static final Pattern GODKJENNING_PATTERN = compilePattern("godkjenning");
  private static final Pattern ORIENTERING_PATTERN =
      compilePattern("orienteringssak", "orientering");
  private static final Pattern REFERAT_PATTERN = compilePattern("referatsak", "referat");

  public static MoetesakDTO.MoetesakstypeEnum resolve(String type) {
    if (type == null) {
      return MoetesakDTO.MoetesakstypeEnum.ANNET;
    }

    if (MOETE_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.MOETE;
    }
    if (POLITISK_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.POLITISK;
    }
    if (DELEGERT_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.DELEGERT;
    }
    if (INTERPELLASJON_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.INTERPELLASJON;
    }
    if (GODKJENNING_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.GODKJENNING;
    }
    if (ORIENTERING_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.ORIENTERING;
    }
    if (REFERAT_PATTERN.matcher(type).find()) {
      return MoetesakDTO.MoetesakstypeEnum.REFERAT;
    }

    return MoetesakDTO.MoetesakstypeEnum.ANNET;
  }

  private static Pattern compilePattern(String... parts) {
    var processedParts = new String[parts.length];
    for (int i = 0; i < parts.length; i++) {
      processedParts[i] =
          parts[i]
              // Replace ø with pattern that matches ø, o, oe and URL-encoded variants
              .replaceAll("ø", "(\u00F8|o|oe|%c3%b8|%c3%98)")
              // Replace å with pattern that matches å, a, aa and URL-encoded variants
              .replaceAll("å", "(\u00E5|a|aa|%c3%a5|%c3%85)")
              // Replace spaces with pattern that matches space, underscore, URL-encoded space, or +
              .replaceAll(" ", "( |_|%20|\\\\+)?");
    }
    return Pattern.compile(
        String.join("|", processedParts), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }
}
