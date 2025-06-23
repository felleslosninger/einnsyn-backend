package no.einnsyn.backend.entities.moetesak.models;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MoetesakstypeResolver {

  private MoetesakstypeResolver() {
    throw new IllegalStateException("Utility class");
  }

  // Pre-compile patterns for matching moetesak types
  private static final Map<Pattern, MoetesakDTO.MoetesakstypeEnum> PATTERNS = new LinkedHashMap<>();

  private static final String OE = "(\u00F8|o|oe)"; // Ø
  private static final String AA = "(\u00E5|a|aa)"; // Å
  private static final String SPACE = "( |_)?"; // Optional space / underscore

  static {
    PATTERNS.put(
        pattern("m" + OE + "tesakstype" + SPACE + "annet", "Notater og orienteringer"),
        MoetesakDTO.MoetesakstypeEnum.MOETE);
    PATTERNS.put(
        pattern(
            "politisk" + SPACE + "sak",
            "klagenemnda",
            "faste" + SPACE + "saker",
            "byr" + AA + "dssak"),
        MoetesakDTO.MoetesakstypeEnum.POLITISK);
    PATTERNS.put(
        pattern("delegert" + SPACE + "sak", "administrativ" + SPACE + "sak", "delegert"),
        MoetesakDTO.MoetesakstypeEnum.DELEGERT);
    PATTERNS.put(pattern("interpellasjon"), MoetesakDTO.MoetesakstypeEnum.INTERPELLASJON);
    PATTERNS.put(pattern("godkjenning"), MoetesakDTO.MoetesakstypeEnum.GODKJENNING);
    PATTERNS.put(
        pattern("orienteringssak", "orientering"), MoetesakDTO.MoetesakstypeEnum.ORIENTERING);
    PATTERNS.put(pattern("referatsak", "referat"), MoetesakDTO.MoetesakstypeEnum.REFERAT);
  }

  public static MoetesakDTO.MoetesakstypeEnum resolve(String type) {
    if (type == null) {
      return MoetesakDTO.MoetesakstypeEnum.ANNET;
    }

    String decodedType;
    try {
      decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      decodedType = type;
    }

    for (Map.Entry<Pattern, MoetesakDTO.MoetesakstypeEnum> entry : PATTERNS.entrySet()) {
      if (entry.getKey().matcher(decodedType).find()) {
        return entry.getValue();
      }
    }

    return MoetesakDTO.MoetesakstypeEnum.ANNET;
  }

  private static Pattern pattern(String... parts) {
    return Pattern.compile(String.join("|", parts), Pattern.CASE_INSENSITIVE);
  }
}
