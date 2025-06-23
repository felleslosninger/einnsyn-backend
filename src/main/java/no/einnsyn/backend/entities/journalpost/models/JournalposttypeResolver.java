package no.einnsyn.backend.entities.journalpost.models;

import java.util.regex.Pattern;

public class JournalposttypeResolver {

  private JournalposttypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  private static final Pattern INNGAAENDE_DOKUMENT_PATTERN =
      compilePattern("inn(gående|kommende) dokument");
  private static final Pattern UTGAAENDE_DOKUMENT_PATTERN = compilePattern("utgående dokument");
  private static final Pattern ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING_PATTERN =
      compilePattern("organinternt dokument uten oppfølgn?ing");
  private static final Pattern ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING_PATTERN =
      compilePattern("organinternt dokument for oppfølgn?ing");
  private static final Pattern SAKSFRAMLEGG_PATTERN = compilePattern("saksframlegg");
  private static final Pattern SAKSKART_PATTERN = compilePattern("sakskart");
  private static final Pattern MOETEPROTOKOLL_PATTERN = compilePattern("møteprotokoll");
  private static final Pattern MOETEBOK_PATTERN = compilePattern("møtebok");

  public static JournalpostDTO.JournalposttypeEnum resolve(String type) {
    if (type == null) {
      return JournalpostDTO.JournalposttypeEnum.UKJENT;
    }

    if (INNGAAENDE_DOKUMENT_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.INNGAAENDE_DOKUMENT;
    }
    if (UTGAAENDE_DOKUMENT_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.UTGAAENDE_DOKUMENT;
    }
    if (ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING;
    }
    if (ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING;
    }
    if (SAKSFRAMLEGG_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.SAKSFRAMLEGG;
    }
    if (SAKSKART_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.SAKSKART;
    }
    if (MOETEPROTOKOLL_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.MOETEPROTOKOLL;
    }
    if (MOETEBOK_PATTERN.matcher(type).find()) {
      return JournalpostDTO.JournalposttypeEnum.MOETEBOK;
    }

    return JournalpostDTO.JournalposttypeEnum.UKJENT;
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
    return Pattern.compile(String.join("|", processedParts), Pattern.CASE_INSENSITIVE);
  }
}
