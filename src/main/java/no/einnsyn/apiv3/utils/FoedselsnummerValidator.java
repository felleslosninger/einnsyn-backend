package no.einnsyn.apiv3.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public class FoedselsnummerValidator {

  // Pre-compile the regex
  private static Pattern pattern = Pattern.compile("^\\d{11}$");

  public static boolean isValid(String nummer) {
    if (nummer == null) {
      return false;
    } else if (!pattern.matcher(nummer).matches()) {
      return false;
    }

    String date = nummer.substring(0, 6);
    if (!isValidDate(date)) {
      return false;
    }

    int[] nummerArray = new int[11];
    for (int i = 0; i < 11; i++) {
      nummerArray[i] = Character.getNumericValue(nummer.charAt(i));
    }

    // @formatter:off
    int k1 = 11 - (
      3 * nummerArray[0] +
      7 * nummerArray[1] +
      6 * nummerArray[2] +
      1 * nummerArray[3] +
      8 * nummerArray[4] +
      9 * nummerArray[5] +
      4 * nummerArray[6] +
      5 * nummerArray[7] +
      2 * nummerArray[8]
    ) % 11;
    // @formatter:on

    if (k1 == 11) {
      k1 = 0;
    } else if (k1 != nummerArray[9]) {
      return false;
    }

    // @formatter:off
    int k2 = 11 - (
      5 * nummerArray[0] +
      4 * nummerArray[1] +
      3 * nummerArray[2] +
      2 * nummerArray[3] +
      7 * nummerArray[4] +
      6 * nummerArray[5] +
      5 * nummerArray[6] +
      4 * nummerArray[7] +
      3 * nummerArray[8] +
      2 * k1
    ) % 11;
    // @formatter:on

    if (k2 == 11) {
      k2 = 0;
    } else if (k2 != nummerArray[10]) {
      return false;
    }

    return true;
  }


  /**
   * Checks if the date string is valid
   * 
   * @param date
   * @return
   */
  private static boolean isValidDate(String date) {
    try {
      LocalDate.parse(date, DateTimeFormatter.ofPattern("ddMMyy"));
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

}
