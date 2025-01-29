package no.einnsyn.backend.entities.journalpost.models;

import java.util.regex.Pattern;

public class JournalposttypeResolver {

  private JournalposttypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  // Pre-compile patterns for matching journalpost types
  private static final String OE = "(ø|o|%c3%b8|%c3%98)";
  private static final String AA = "(å|a|aa|%c3%a5|%c3%85)";
  private static final Pattern PAT_INNGAAENDE_DOKUMENT =
      Pattern.compile(".*inn(g" + AA + "ende|kommende)dokument", Pattern.CANON_EQ);
  private static final Pattern PAT_UTGAAENDE_DOKUMENT =
      Pattern.compile(".*utg" + AA + "endedokument", Pattern.CANON_EQ);
  private static final Pattern PAT_ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING =
      Pattern.compile(".*organinterntdokumentutenoppf" + OE + "lgn?ing", Pattern.CANON_EQ);
  private static final Pattern PAT_ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING =
      Pattern.compile(".*organinterntdokumentforoppf" + OE + "lgn?ing", Pattern.CANON_EQ);
  private static final Pattern PAT_SAKSFRAMLEGG = Pattern.compile(".*saksframlegg");
  private static final Pattern PAT_SAKSKART = Pattern.compile(".*sakskart");
  private static final Pattern PAT_MOETEPROTOKOLL =
      Pattern.compile(".*m" + OE + "teprotokoll", Pattern.CANON_EQ);
  private static final Pattern PAT_MOETEBOK =
      Pattern.compile(".*m" + OE + "tebok", Pattern.CANON_EQ);

  public static JournalpostDTO.JournalposttypeEnum resolve(String type) {
    type = type.toLowerCase();
    if (PAT_INNGAAENDE_DOKUMENT.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.INNGAAENDE_DOKUMENT;
    }
    if (PAT_UTGAAENDE_DOKUMENT.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.UTGAAENDE_DOKUMENT;
    }
    if (PAT_ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.ORGANINTERNT_DOKUMENT_UTEN_OPPFOELGING;
    }
    if (PAT_ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.ORGANINTERNT_DOKUMENT_FOR_OPPFOELGING;
    }
    if (PAT_SAKSFRAMLEGG.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.SAKSFRAMLEGG;
    }
    if (PAT_SAKSKART.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.SAKSKART;
    }
    if (PAT_MOETEPROTOKOLL.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.MOETEPROTOKOLL;
    }
    if (PAT_MOETEBOK.matcher(type).matches()) {
      return JournalpostDTO.JournalposttypeEnum.MOETEBOK;
    }
    return JournalpostDTO.JournalposttypeEnum.UKJENT;
  }
}
