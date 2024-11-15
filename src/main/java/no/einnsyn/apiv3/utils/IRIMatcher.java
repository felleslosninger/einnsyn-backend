package no.einnsyn.apiv3.utils;

import java.util.regex.Pattern;

public class IRIMatcher {

  private static Pattern pattern = Pattern.compile("^https?://.*", Pattern.CASE_INSENSITIVE);

  public static boolean matches(String iri) {
    return pattern.matcher(iri).matches();
  }
}
