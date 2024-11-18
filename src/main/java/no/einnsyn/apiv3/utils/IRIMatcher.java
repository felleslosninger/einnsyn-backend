package no.einnsyn.apiv3.utils;

import java.util.regex.Pattern;

/**
 * The {@code IRIMatcher} class provides a utility method to check if a given string is a valid IRI.
 */
public class IRIMatcher {

  // A pre-compiled pattern that should match all IRIs
  private static Pattern pattern = Pattern.compile("^[a-zA-Z][a-zA-Z+.-]*://[a-zA-Z0-9]+");

  /**
   * Checks if the provided string is an IRI.
   *
   * @param iri the string to be checked against the IRI pattern
   * @return {@code true} if the string matches the IRI pattern, {@code false} otherwise
   */
  public static boolean matches(String iri) {
    return pattern.matcher(iri).matches();
  }
}
