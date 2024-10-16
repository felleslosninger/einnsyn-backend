package no.einnsyn.apiv3.utils.idgenerator;

public class IdValidator {

  IdValidator() {}

  // Build a regex pattern from the map keys
  public static final String ID_PATTERN =
      ""
          + "("
          + "("
          + String.join("|", IdPrefix.map.values())
          + ")"
          + "_"
          + "[0123456789abcdefghjkmnpqrstvwxyz]{26}"
          + ")";

  public static boolean isValid(String id) {
    return id != null && id.matches("^" + ID_PATTERN + "$");
  }
}
