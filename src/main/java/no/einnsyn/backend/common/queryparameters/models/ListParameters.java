// Auto-generated from our API specification
// https://github.com/felleslosninger/einnsyn-api

package no.einnsyn.backend.common.queryparameters.models;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import no.einnsyn.backend.validation.validenum.ValidEnum;

@Getter
@Setter
public class ListParameters {
  /** Specifies which fields in the response should be expanded. */
  List<String> expand;

  /**
   * A limit on the number of objects to be returned. Limit can range between 1 and 100, and the
   * default is 10.
   */
  Integer limit = 25;

  /** The sort order of the result set. The default is ascending. */
  @ValidEnum(enumClass = SortOrderEnum.class)
  String sortOrder;

  /**
   * A cursor for use in pagination. StartingAfter is a resource ID that defines your place in the
   * list.
   */
  String startingAfter;

  /**
   * A cursor for use in pagination. EndingBefore is a resource ID that defines your place in the
   * list.
   */
  String endingBefore;

  /**
   * A list of resource IDs to be returned. If this parameter is used, the other parameters will be
   * ignored.
   */
  List<String> ids;

  /**
   * A list of external IDs to be returned. If this parameter is used, the other parameters will be
   * ignored.
   */
  List<String> externalIds;

  /** The Journalenhet to filter the result set by. */
  String journalenhet;

  public enum SortOrderEnum {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortOrderEnum(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    public String toJson() {
      return value;
    }

    public static SortOrderEnum fromValue(String value) {
      value = value.trim().toLowerCase();
      for (SortOrderEnum val : SortOrderEnum.values()) {
        if (val.value.toLowerCase().equals(value)) {
          return val;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
