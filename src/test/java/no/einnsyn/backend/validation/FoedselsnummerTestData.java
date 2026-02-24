package no.einnsyn.backend.validation;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import no.bekk.bekkopen.person.Fodselsnummer;
import no.bekk.bekkopen.person.FodselsnummerCalculator;
import no.bekk.bekkopen.person.KJONN;

final class FoedselsnummerTestData {

  static final String REFERENCE_FOEDSELSNUMMER =
      getFoedselsnummerFor(LocalDate.of(1985, 6, 5), KJONN.MANN);

  private FoedselsnummerTestData() {}

  static List<String> validFoedselsnummers() {
    var generatedFoedselsnummers = new LinkedHashSet<String>();

    // Regular foedselsnummer with explicit male/female variants.
    generatedFoedselsnummers.add(REFERENCE_FOEDSELSNUMMER);
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(1985, 6, 5), KJONN.KVINNE));

    // Leap day and century boundaries.
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(2000, 2, 29), KJONN.MANN));
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(2000, 2, 29), KJONN.KVINNE));
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(1999, 12, 31), KJONN.MANN));
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(2000, 1, 1), KJONN.KVINNE));

    // 18xx era edge case and D-number variants.
    generatedFoedselsnummers.add(getFoedselsnummerFor(LocalDate.of(1854, 1, 1), KJONN.MANN));
    generatedFoedselsnummers.add(getDNumberFor(LocalDate.of(2010, 8, 15), KJONN.MANN));
    generatedFoedselsnummers.add(getDNumberFor(LocalDate.of(2010, 8, 15), KJONN.KVINNE));

    // Valid formatting variants accepted by NoSSNValidator pattern.
    generatedFoedselsnummers.add(withSpaceAfterDate(REFERENCE_FOEDSELSNUMMER));
    generatedFoedselsnummers.add(withDateSeparators(REFERENCE_FOEDSELSNUMMER, "."));
    generatedFoedselsnummers.add(withDateSeparators(REFERENCE_FOEDSELSNUMMER, " "));

    return List.copyOf(generatedFoedselsnummers);
  }

  static List<String> invalidFoedselsnummers() {
    return List.of(
        "3108531a449", // Contains non-integer character
        "32038738758", // 32 days in month
        "03042535428", // Wrong k1
        "04041870709", // Wrong k2
        "08131939521", // 13 months
        "2807784800", // Too few integers
        "15051802111", // Invalid checksum
        "02120818202", // Invalid checksum
        "10046038375", // Invalid checksum
        "20060810012", // Invalid checksum
        "0" + REFERENCE_FOEDSELSNUMMER, // Valid, with leading number
        REFERENCE_FOEDSELSNUMMER + "0", // Valid, with trailing number
        "13da68dd-6c0c-591f-a183-" + REFERENCE_FOEDSELSNUMMER + "a", // Valid, but part of an UUID
        "13da68dd-6c0c-591f-a183-a" + REFERENCE_FOEDSELSNUMMER, // Valid, but part of an UUID
        "D1A11529277DADC9BF7EACEBC12072480617B4E29C2F82BFB5C9D701A4E8C11B");
  }

  private static String getFoedselsnummerFor(LocalDate date, KJONN kjonn) {
    return FodselsnummerCalculator.getFodselsnummerForDateAndGender(Date.valueOf(date), kjonn)
        .stream()
        .map(Fodselsnummer::toString)
        .sorted()
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Could not generate foedselsnummer for date=" + date + ", gender=" + kjonn));
  }

  private static String getDNumberFor(LocalDate date, KJONN kjonn) {
    return FodselsnummerCalculator.getManyDNumberFodselsnummerForDate(Date.valueOf(date)).stream()
        .filter(fodselsnummer -> fodselsnummer.getKjonn() == kjonn)
        .map(Fodselsnummer::toString)
        .sorted()
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Could not generate D-number for date=" + date + ", gender=" + kjonn));
  }

  private static String withSpaceAfterDate(String foedselsnummer) {
    return foedselsnummer.substring(0, 6) + " " + foedselsnummer.substring(6);
  }

  private static String withDateSeparators(String foedselsnummer, String separator) {
    return foedselsnummer.substring(0, 4)
        + separator
        + foedselsnummer.substring(4, 6)
        + separator
        + foedselsnummer.substring(6);
  }
}
